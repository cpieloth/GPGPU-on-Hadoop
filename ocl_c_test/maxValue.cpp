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

size_t size;
char type;

/*** Kommandozeilenargumente ***/
const char SINGLE = 's';
const char CPU = 'c';
const char GPU = 'g';

const char ALL = 'a';
const char DEBUG = 'd';
const char NORMAL = 'n';
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
  /*** Erstelle OpenCL-Context und CommandQueue ***/
  context = cl::Context(CL_TYPE);
  devices = context.getInfo<CL_CONTEXT_DEVICES> ();
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
      Logger::logInfo(
          METHOD,
          Logger::sStream << "max compute units: " << devices[0].getInfo<
              CL_DEVICE_MAX_COMPUTE_UNITS> ());
      Logger::logInfo(
          METHOD,
          Logger::sStream << "max work item sizes: " << devices[0].getInfo<
              CL_DEVICE_MAX_WORK_ITEM_SIZES> ()[0]);
      Logger::logInfo(
          METHOD,
          Logger::sStream << "max work group sizes: " << devices[0].getInfo<
              CL_DEVICE_MAX_WORK_GROUP_SIZE> ());
      Logger::logInfo(
          METHOD,
          Logger::sStream << "max global mem size (KB): "
              << devices[0].getInfo<CL_DEVICE_GLOBAL_MEM_SIZE> () / 1024);
      Logger::logInfo(
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
          status = kernel.setArg(1, (cl_uint)(len));
          if (status != CL_SUCCESS)
            {
              throw cl::Error(status, "Kernel.SetArg");
            }

          status = kernel.setArg(2, sizeof(cl_int) * localSize, NULL);
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

void
fillVector(int* vec, const size_t SIZE)
{
  srand(time(NULL));
  for (size_t i = 0; i < SIZE; ++i)
    {
      vec[i] = rand() % 1024;
    }
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
  type = *argv[2];
  size = atoi(argv[3]);

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

int
main(int argc, char** argv)
{
  if (!checkArguments(argc, argv))
    return EXIT_FAILURE;

  const std::string METHOD("main");
  double runtime = -1.0;

  Logger::logInfo(METHOD, Logger::sStream << "type = " << type);
  Logger::logInfo(METHOD, Logger::sStream << "size = " << size);

  /*** Erzeugen der Daten***/
  if (size % WG_FAC != 0)
    {
      size = ceil((double) size / WG_FAC) * WG_FAC;
      Logger::logWarn(METHOD,
          Logger::sStream << "Array has been extended to " << size);
    }

  Logger::logInfo(METHOD,
      Logger::sStream << "RAM (KB) > " << (size * sizeof(int)) / 1024);

  int* values = (int*) malloc(size * sizeof(int));

  fillVector(values, size);

  /*** Kontrollwerte ***/
  values[(size_t) (size / 3)] = 2323;
  values[(size_t) (size / 2)] = 4242;
  values[size - 1] = 7331;

  Logger::logInfo(METHOD,
      Logger::sStream << getString(values, size > PRT_CNT ? PRT_CNT : size));

  /*** Implementierung waehlen ***/
  switch (type)
    {
  case SINGLE:
    runtime = maxValue(values, size);
    break;
  case GPU:
    runtime = maxValueCL(CL_DEVICE_TYPE_GPU, values, size);
    break;
  case CPU:
    runtime = maxValueCL(CL_DEVICE_TYPE_CPU, values, size);
    break;
  default:
    ;
    Logger::logError(METHOD,
        Logger::sStream << "Unknown type: \"" << type << "\"");
    }

  if (runtime > -1)
    Logger::logInfo(METHOD,
        Logger::sStream << getString(values, size > PRT_CNT ? PRT_CNT : size));

  Logger::log(METHOD, TIME, Logger::sStream << "time=" << runtime << ";");

  free(values);

  return EXIT_SUCCESS;
}
