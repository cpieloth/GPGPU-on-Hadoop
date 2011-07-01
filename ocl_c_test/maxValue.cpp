#ifndef KERNEL_PATH
#define KERNEL_PATH "kernel.cl"
#endif

#ifndef __CL_ENABLE_EXCEPTIONS
#define __CL_ENABLE_EXCEPTIONS
#endif

#define PRT_CNT 8
#define WG_FAC 64

#include "CL/cl.hpp"

#include <iostream>
#include <sstream>
#include <fstream>
#include <algorithm>
#include <math.h>

#include "logging.hpp"
#include "timeUtils.hpp"

using namespace logging;

size_t _size;
char _type;

/*** Kommandozeilenargumente ***/
const char SINGLE = 's';
const char CPU = 'c';
const char GPU = 'g';

const char ALL = 'a';
const char DEBUG = 'd';
const char NORMAL = 'n';
const char INFO = 'i';
const char ERRORL = 'e';
const char TIMEL = 't';

const logging::Level TIME("TIME", 32);

std::string
getString(int* vec, const size_t SIZE)
{
  std::stringstream sstream;
  for (size_t i = 0; i < SIZE; i++)
    {
      sstream << vec[i] << ", ";
    }
  return sstream.str();
}

std::string
readFile(std::string fName)
{
  std::ifstream file;
  file.open(fName.c_str(), std::ios::in);

  if (!file)
    std::cerr << "Could not open file: " << fName << std::endl;

  char c;

  std::string kernelSrc;
  while (file.get(c))
    kernelSrc.push_back(c);
  file.close();
  return kernelSrc;
}

// TODO Berechnung fuer optimale Work-Group-Size optimieren
void
calcWorkGroupSize(size_t& globalSize, size_t& localSize,
    const size_t MAX_GROUP_SIZE)
{
  localSize = MAX_GROUP_SIZE;
  if (globalSize < localSize)
    localSize = globalSize;
  else
    while (globalSize % localSize != 0)
      --localSize;
}

double
maxValue(int* values, const size_t LEN)
{
  timeUtils::Clock timer;
  timer.start();

  for (size_t i = 1; i < LEN; i++)
    values[0] = std::max(values[0], values[i]);

  timer.stop();
  return timer.getTimeInSeconds();
}

void
initCL(cl::Context & context, const cl_device_type CL_TYPE,
    std::vector<cl::Device> & devices, cl::CommandQueue & cmdQ,
    cl::Program & program, cl::Kernel & kernel)
{
  /*** Hole OpenCL-Plattformen z.B. AMD APP, NVIDIA CUDA ***/
  std::vector<cl::Platform> platforms;
  cl::Platform::get(&platforms);

  /*** Hole OpenCL-Device des geforderten Typs z.B. GPU, CPU ***/
  std::vector<cl::Device> devTmp;
  for (std::vector<cl::Platform>::iterator it = platforms.begin(); it
      != platforms.end(); ++it)
    {
      it->getDevices(CL_TYPE, &devTmp);
      devices.insert(devices.end(), devTmp.begin(), devTmp.end());
      devTmp.clear();
    }

  /*** Erstelle OpenCL-Context und CommandQueue ***/
  context = cl::Context(devices);
  cmdQ = cl::CommandQueue(context, devices[0], CL_QUEUE_PROFILING_ENABLE);

  /*** OpenCL-Quellcode einlesen ***/
  std::string src = readFile(KERNEL_PATH);
  cl::Program::Sources source;
  source.push_back(std::make_pair(src.data(), src.length()));

  /*** OpenCL-Programm aus Quellcode erstellen ***/
  program = cl::Program(context, source);
  try
    {
      program.build(devices);
    }
  catch (cl::Error & err)
    {
      ;
      Logger::logDebug(
          "initCL",
          Logger::sStream << err.what() << "\nBuild-Log fuer \""
              << devices.front().getInfo<CL_DEVICE_NAME> () << "\":\n"
              << program.getBuildInfo<CL_PROGRAM_BUILD_LOG> (devices.front()));
      throw err;
    }
  kernel = cl::Kernel(program, "maxInt");
}

double
maxValueCL(const cl_device_type CL_TYPE, int* values, size_t len)
{
  const std::string METHOD("maxValueCL");
  timeUtils::Clock timer;
  double runtimeKernel = 0;

  std::vector<cl::Platform> platforms;
  std::vector<cl::Device> devices;
  cl::Context context;
  cl::Program program;
  cl::Program::Sources source;
  cl::Kernel kernel;
  cl::CommandQueue cmdQ;

  try
    {
      cl_int status = CL_SUCCESS;
      initCL(context, CL_TYPE, devices, cmdQ, program, kernel);

      /*** Ausgabe von Informationen ueber gewaehltes OpenCL-Device ***/
      Logger::logDebug(
          METHOD,
          Logger::sStream << "max compute units: " << devices[0].getInfo<
              CL_DEVICE_MAX_COMPUTE_UNITS> ());
      Logger::logDebug(
          METHOD,
          Logger::sStream << "max work item sizes: " << devices[0].getInfo<
              CL_DEVICE_MAX_WORK_ITEM_SIZES> ()[0]);
      Logger::logDebug(
          METHOD,
          Logger::sStream << "max work group sizes: " << devices[0].getInfo<
              CL_DEVICE_MAX_WORK_GROUP_SIZE> ());
      Logger::logDebug(
          METHOD,
          Logger::sStream << "max global mem size (KB): "
              << devices[0].getInfo<CL_DEVICE_GLOBAL_MEM_SIZE> () / 1024);
      Logger::logDebug(
          METHOD,
          Logger::sStream << "max local mem size (KB): " << devices[0].getInfo<
              CL_DEVICE_LOCAL_MEM_SIZE> () / 1024);

      /*** Erstellen und Vorbereiten der Daten ***/
      timer.start();
      cl::Buffer vBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
          sizeof(cl_int) * len, &values[0], &status);
      if (status != CL_SUCCESS)
        {
          throw cl::Error(status, "cl::Buffer values");
        }
      cmdQ.finish();

      /*** Arbeitsgrš§en berechnen ***/
      // Anzahl der Work-Items = globalSize
      // Work-Items pro Work-Group = localSize
      const size_t MAX_GROUP_SIZE = devices[0].getInfo<
          CL_DEVICE_MAX_WORK_GROUP_SIZE> ();
      size_t globalSize;
      size_t localSize;

      do
        {
          globalSize = len;
          calcWorkGroupSize(globalSize, localSize, MAX_GROUP_SIZE);
          if (localSize == 1)
            {
              globalSize = ceil((double) len / WG_FAC) * WG_FAC;
              calcWorkGroupSize(globalSize, localSize, MAX_GROUP_SIZE);
              Logger::logDebug(
                  METHOD,
                  Logger::sStream << "GlobalSize has been extended to "
                      << globalSize);
            }

          Logger::logDebug(METHOD,
              Logger::sStream << "globalSize: " << globalSize);
          Logger::logDebug(METHOD,
              Logger::sStream << "localSize: " << localSize);

          /*** Kernel-Argumente setzen  ***/
          status = kernel.setArg(0, vBuffer);
          if (status != CL_SUCCESS)
            {
              throw cl::Error(status, "Kernel.SetArg");
            }

          status = kernel.setArg(1, sizeof(cl_int) * localSize, NULL);
          if (status != CL_SUCCESS)
            {
              throw cl::Error(status, "Kernel.SetArg");
            }

          /*** Kernel ausfuehren und auf Abarbeitung warten ***/

          cl::KernelFunctor func = kernel.bind(cmdQ, cl::NDRange(globalSize),
              cl::NDRange(localSize));

          cl::Event event = func();

          cmdQ.finish();

          runtimeKernel += event.getProfilingInfo<CL_PROFILING_COMMAND_END> ();
          runtimeKernel
              -= event.getProfilingInfo<CL_PROFILING_COMMAND_START> ();

          len = globalSize / localSize;
        }
      while (globalSize > localSize && localSize > 1);

      /*** Daten vom OpenCL-Device holen ***/
      status = cmdQ.enqueueReadBuffer(vBuffer, true, 0, sizeof(cl_int) * len,
          &values[0]);
      if (status != CL_SUCCESS)
        {
          throw cl::Error(status, "CommandQueue.enqueueReadBuffer");
        }

      timer.stop();

      Logger::log(METHOD, TIME,
          Logger::sStream << "timeKernel=" << 1.0e-9 * runtimeKernel << ";");
    }
  catch (cl::Error& err)
    {
      Logger::logError(METHOD, Logger::sStream << err.what());
    }
  catch (std::exception& err)
    {
      Logger::logError(METHOD, Logger::sStream << err.what());
    }

  return timer.getTimeInSeconds();
}

bool
checkArguments(int argc, char** argv)
{
  if (argc < 4)
    {
      std::cout << "Arguments: " << ALL << "|" << DEBUG << "|" << NORMAL << "|"
          << TIMEL << "|" << ERRORL << " " << SINGLE << "|" << CPU << "|"
          << GPU << " <size>" << std::endl;
      return false;
    }

  char log = *argv[1];
  _type = *argv[2];
  _size = atoi(argv[3]);

  switch (log)
    {
  case ALL:
    Logger::setLogMask(Level::ALL);
    break;
  case DEBUG:
    Logger::setLogMask(Level::DEBUG);
    break;
  case NORMAL:
    Logger::setLogMask(Level::NORMAL);
    break;
  case INFO:
      Logger::setLogMask(Level::INFO);
      break;
  case TIMEL:
    Logger::setLogMask(TIME);
    break;
  case ERRORL:
    Logger::setLogMask(Level::ERR);
    break;
  default:
    Logger::setLogMask(Level::NORMAL);
    }

  return true;
}

int *
prepareData(size_t& size)
{
  const std::string METHOD("prepareData");
  /*** Erzeugen der Daten***/
  if (size % WG_FAC != 0)
    {
      size = ceil((double) size / WG_FAC) * WG_FAC;
      Logger::logWarn(METHOD,
          Logger::sStream << "Array has been extended to " << size);
    }
  Logger::logInfo(METHOD,
      Logger::sStream << "RAM (KB) > " << (size * sizeof(int)) / 1024);
  int *values = (int*) (malloc(size * sizeof(int)));

  srand(time(NULL));
  for (size_t i = 0; i < size; ++i)
    {
      values[i] = rand() % (size / 2);
    }

  return values;
}

size_t
setTestValues(int* values, const size_t SIZE)
{
  size_t max_pos = rand() % SIZE;

  int max_val = 9;
  const size_t MAGNITUTE = ceil(log10(SIZE));
  for (size_t i = 0; i < MAGNITUTE; i++)
    max_val = max_val * 10 + 9;

  values[max_pos] = max_val;
  return max_pos;
}

bool
test(const char TYPE, const size_t START, const size_t END,
    const size_t STEP)
{
  const std::string METHOD("test");
  bool success = true;
  int* values;

  /*** Testwerte setzen ***/
  size_t max_pos = -1;
  int max_val = -1;
  double runtime = -1;

  for (size_t i = START; i < END; i += STEP)
    {
      values = prepareData(i);
      max_pos = setTestValues(values, i);
      max_val = values[max_pos];

      switch (TYPE)
        {
      case SINGLE:
        runtime = maxValue(values, i);
        break;
      case GPU:
        runtime = maxValueCL(CL_DEVICE_TYPE_GPU, values, i);
        break;
      case CPU:
        runtime = maxValueCL(CL_DEVICE_TYPE_CPU, values, i);
        break;
      default:
        ;
        Logger::logError(METHOD,
            Logger::sStream << "Unknown type: \"" << _type << "\"");
        }

      success = values[0] == max_val;

      Logger::logInfo(
          METHOD,
          Logger::sStream << (success ? "PASSED " : "FAILED ")
              << "size: " << i << "; max_pos: " << max_pos << "; max_val: "
              << max_val << "; values[0]: " << values[0] << "; runtime: " << runtime);

      free(values);

      if(!success)
        break;
    }

  return success;
}

int
main(int argc, char** argv)
{
  if (!checkArguments(argc, argv))
    return EXIT_FAILURE;

  const std::string METHOD("main");
  double runtime = -1.0;

  Logger::logInfo(METHOD, Logger::sStream << "type = " << _type);
  Logger::logInfo(METHOD, Logger::sStream << "size = " << _size);

  /*** Test ***/
#ifdef TEST
  bool success = test(_type, WG_FAC, _size, (_size / 32) + 1);
  Logger::logInfo(METHOD, Logger::sStream << "TEST: " << (success ? "PASSED " : "FAILED "));
  return success ? EXIT_SUCCESS : EXIT_FAILURE;
#endif

  /**** Array fuellen ***/
  int *values = prepareData(_size);
  setTestValues(values, _size);

  Logger::logInfo(METHOD,
      Logger::sStream << getString(values, _size > PRT_CNT ? PRT_CNT : _size));

  /*** Implementierung waehlen ***/
  switch (_type)
    {
  case SINGLE:
    runtime = maxValue(values, _size);
    break;
  case GPU:
    runtime = maxValueCL(CL_DEVICE_TYPE_GPU, values, _size);
    break;
  case CPU:
    runtime = maxValueCL(CL_DEVICE_TYPE_CPU, values, _size);
    break;
  default:
    ;
    Logger::logError(METHOD,
        Logger::sStream << "Unknown type: \"" << _type << "\"");
    }

  if (runtime > -1)
    Logger::logInfo(METHOD,
        Logger::sStream << getString(values, _size > PRT_CNT ? PRT_CNT : _size));

  // Logger::logInfo(METHOD, Logger::sStream << "max_pos: " << max_pos);
  Logger::log(METHOD, TIME, Logger::sStream << "time=" << runtime << ";");

  free(values);

  return EXIT_SUCCESS;
}
