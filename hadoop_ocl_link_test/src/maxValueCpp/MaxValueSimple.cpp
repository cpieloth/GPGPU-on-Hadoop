#ifndef KERNEL_PATH
#define KERNEL_PATH "kernel.cl"
#endif

#ifndef __CL_ENABLE_EXCEPTIONS
#define __CL_ENABLE_EXCEPTIONS
#endif

#include "CL/cl.hpp"

#include <limits>
#include <iostream>
#include <sstream>
#include <fstream>
#include <algorithm>
#include <math.h>

class MaxValueSimple {
public:
	MaxValueSimple(int argc, char** argv);
	MaxValueSimple();
	bool initialize();
	bool initialize(cl_device_type type);
	void finalize();
	int maxValue(int* values, const size_t LEN);

	int* prepareData(size_t& size);
	size_t setTestValues(int* values, const size_t SIZE);

	/* Kommandozeilenargumente */
	static const char CPU = 's';
	static const char CLCPU = 'c';
	static const char CLGPU = 'g';

	static const char INFO = 'i';
	static const char ERROR = 'e';
	static const char DEBUG = 'd';

	/* Globale Variablen */
	size_t LEN;
	char TYPE;
	static int MAX_FAILURE;

private:
	int maxValueCL(int* values, size_t len);
	int calcWorkGroupSize(int globalSize, const long MAX_GROUP_SIZE);
	std::string readFile(std::string file);
	bool checkArguments(int argc, char** argv);

	static const int WG_FAC = 64;
	std::vector<cl::Platform> platforms;
	std::vector<cl::Device> devices;
	cl::Context context;
	cl::CommandQueue cmdQ;
	cl::Program program;
	cl::Kernel kernel;
	cl::Event event;
};

int MaxValueSimple::MAX_FAILURE = std::numeric_limits<int>::min();

MaxValueSimple::MaxValueSimple(int argc, char** argv) {
	LEN = 0;
	TYPE = CPU;

	MaxValueSimple::checkArguments(argc, argv);
}

MaxValueSimple::MaxValueSimple() {
	LEN = 1;
	TYPE = CPU;
	// TODO Logging
}

bool MaxValueSimple::checkArguments(int argc, char** argv) {
	if (argc < 4) {
		std::cout << "Argumente: ";
		std::cout << INFO << "|" << DEBUG << "|" << ERROR;
		std::cout << " " << CLCPU << "|" << CLGPU << "|" << CPU;
		std::cout << " <Vektorgroesze>" << std::endl;
		return false;
	}

	/* TODO logging
	 switch (*argv[1]) {
	 case INFO:
	 Logger.setLogMask(Level.DEFAULT.INFO.getLevel().getValue());
	 break;
	 case ERROR:
	 Logger.setLogMask(Level.DEFAULT.ERROR.getLevel().getValue());
	 break;
	 case DEBUG:
	 Logger.setLogMask(Level.DEFAULT.DEBUG.getLevel().getValue());
	 break;
	 default:
	 Logger.setLogMask(Level.DEFAULT.ALL.getLevel().getValue());
	 break;
	 }
	 */
	TYPE = *argv[2];
	LEN = (size_t) atoi(argv[3]);

	return true;
}

bool MaxValueSimple::initialize() {
	cl_device_type type;

	switch (TYPE) {
	case CPU:
		return true;
	case CLCPU:
		type = CL_DEVICE_TYPE_CPU;
		break;
	case CLGPU:
		type = CL_DEVICE_TYPE_GPU;
		break;
	default:
		return false;
	}

	return MaxValueSimple::initialize(type);
}

void MaxValueSimple::finalize() {
	if (TYPE == CPU)
		return;

	/* TODO
	cmdQ.~CommandQueue();
	event.~Event();
	kernel.~Kernel();
	program.~Program();
	context.~Context();
	// TODO platforms
	*/

}

bool MaxValueSimple::initialize(cl_device_type type) {
	if (type == CL_DEVICE_TYPE_CPU)
		TYPE = CLCPU;
	else if (type == CL_DEVICE_TYPE_GPU)
		TYPE = CLGPU;
	else {
		TYPE = CPU;
		return true;
	}

	try {
		/*** Hole OpenCL-Plattformen z.B. AMD APP, NVIDIA CUDA ***/
		cl::Platform::get(&platforms);

		/*** Hole OpenCL-Device des geforderten Typs z.B. GPU, CPU ***/
		std::vector < cl::Device > devTmp;
		for (std::vector<cl::Platform>::iterator it = platforms.begin(); it
				!= platforms.end(); ++it) {
			it->getDevices(type, &devTmp);
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
		try {
			program.build(devices);
		} catch (cl::Error & err) {
			/* TODO logging
			 Logger::logDebug(
			 "initCL",
			 Logger::sStream << err.what() << "\nBuild-Log fuer \""
			 << devices.front().getInfo<CL_DEVICE_NAME> () << "\":\n"
			 << program.getBuildInfo<CL_PROGRAM_BUILD_LOG> (devices.front()));
			 */
			throw err;
		}
		kernel = cl::Kernel(program, "maxInt");
		event = cl::Event();
		return true;
	} catch (cl::Error& err) {
		// TODO Logger::logError(METHOD, Logger::sStream << err.what());
		return false;
	} catch (std::exception& err) {
		// TODO Logger::logError(METHOD, Logger::sStream << err.what());
		return false;
	}
}

std::string MaxValueSimple::readFile(std::string fName) {
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

int MaxValueSimple::maxValue(int* values, const size_t LEN) {
	int max = MaxValueSimple::MAX_FAILURE;

	switch (TYPE) {
	case CPU:
		max = MaxValueSimple::MAX_FAILURE;
		for (size_t i = 0; i < LEN; i++)
			max = std::max(values[i], max);
		values[0] = max;
		break;
	case CLGPU:
		max = MaxValueSimple::maxValueCL(values, LEN);
		break;
	case CLCPU:
		max = MaxValueSimple::maxValueCL(values, LEN);
		break;
	default:
		// TODO Logger.logError(CLAZZ, "Device type not supported!");
		break;
	}

	return max;
}

int MaxValueSimple::maxValueCL(int* values, size_t len) {
	try {
		cl_int status = CL_SUCCESS;

		/*** Ausgabe von Informationen ueber gewaehltes OpenCL-Device ***/
		/* TODO logging
		 Logger::logDebug(
		 METHOD,
		 Logger::sStream << "max compute units: " << devices[0].getInfo<
		 CL_DEVICE_MAX_COMPUTE_UNITS> ());
		 Logger::logDebug(
		 METHOD,
		 Logger::sStream << "max work item sizes: "
		 << devices[0].getInfo<CL_DEVICE_MAX_WORK_ITEM_SIZES> ()[0]);
		 Logger::logDebug(
		 METHOD,
		 Logger::sStream << "max work group sizes: "
		 << devices[0].getInfo<CL_DEVICE_MAX_WORK_GROUP_SIZE> ());
		 Logger::logDebug(
		 METHOD,
		 Logger::sStream << "max global mem size (KB): "
		 << devices[0].getInfo<CL_DEVICE_GLOBAL_MEM_SIZE> ()
		 / 1024);
		 Logger::logDebug(
		 METHOD,
		 Logger::sStream << "max local mem size (KB): "
		 << devices[0].getInfo<CL_DEVICE_LOCAL_MEM_SIZE> ()
		 / 1024);
		 */

		/*** Erstellen und Vorbereiten der Daten ***/
		cl::Buffer vBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
				sizeof(cl_int) * len, &values[0], &status);
		if (status != CL_SUCCESS) {
			throw cl::Error(status, "cl::Buffer values");
		}
		cmdQ.finish();

		/*** Arbeitsgroeszen berechnen ***/
		// Anzahl der Work-Items = globalSize
		// Work-Items pro Work-Group = localSize
		const size_t MAX_GROUP_SIZE = devices[0].getInfo<
				CL_DEVICE_MAX_WORK_GROUP_SIZE> ();
		size_t globalSize;
		size_t localSize;

		do {
			globalSize = len;
			localSize = MaxValueSimple::calcWorkGroupSize(globalSize,
					MAX_GROUP_SIZE);
			if (localSize == 1) {
				globalSize = ceil((double) len / WG_FAC) * WG_FAC;
				localSize = MaxValueSimple::calcWorkGroupSize(globalSize,
						MAX_GROUP_SIZE);
				/* TODO logging
				 Logger::logDebug(
				 METHOD,
				 Logger::sStream << "GlobalSize has been extended to "
				 << globalSize);
				 */
			}
			/* TODO logging
			 Logger::logDebug(METHOD,
			 Logger::sStream << "globalSize: " << globalSize);
			 Logger::logDebug(METHOD,
			 Logger::sStream << "localSize: " << localSize);
			 */

			/*** Kernel-Argumente setzen  ***/
			status = kernel.setArg(0, vBuffer);
			if (status != CL_SUCCESS) {
				throw cl::Error(status, "Kernel.SetArg");
			}

			status = kernel.setArg(1, sizeof(cl_int) * localSize, NULL);
			if (status != CL_SUCCESS) {
				throw cl::Error(status, "Kernel.SetArg");
			}

			/*** Kernel ausfuehren und auf Abarbeitung warten ***/
			cl::KernelFunctor func = kernel.bind(cmdQ, cl::NDRange(globalSize),
					cl::NDRange(localSize));

			event = func();

			event.wait();
			cmdQ.finish();

			/*
			 runtimeKernel
			 += event.getProfilingInfo<CL_PROFILING_COMMAND_END> ();
			 runtimeKernel
			 -= event.getProfilingInfo<CL_PROFILING_COMMAND_START> ();
			 */
			len = globalSize / localSize;
		} while (globalSize > localSize && localSize > 1);

		/*** Daten vom OpenCL-Device holen ***/
		// TODO nur 1. element auslesen
		status = cmdQ.enqueueReadBuffer(vBuffer, true, 0, sizeof(cl_int) * len,
				&values[0]);
		if (status != CL_SUCCESS) {
			throw cl::Error(status, "CommandQueue.enqueueReadBuffer");
		}

		/* TODO logging
		 Logger::log(
		 METHOD,
		 TIME,
		 Logger::sStream << "timeKernel=" << 1.0e-9 * runtimeKernel
		 << ";");
		 */
		return values[0];
	} catch (cl::Error& err) {
		// TODO Logger::logError(METHOD, Logger::sStream << err.what());
		return MaxValueSimple::MAX_FAILURE;
	} catch (std::exception& err) {
		// TODO Logger::logError(METHOD, Logger::sStream << err.what());
		return MaxValueSimple::MAX_FAILURE;
	}
}

int MaxValueSimple::calcWorkGroupSize(int globalSize, const long MAX_GROUP_SIZE) {
	int localSize = (int) MAX_GROUP_SIZE;
	if (globalSize < localSize)
		localSize = globalSize;
	else
		while (globalSize % localSize != 0)
			--localSize;
	return localSize;
}

int* MaxValueSimple::prepareData(size_t& size) {
	// Erzeugen der Daten
	if (size % WG_FAC != 0) {
		size = (int) (ceil((double) size / WG_FAC) * WG_FAC);
	}

	int* values = (int*) (malloc(size * sizeof(int)));

	srand( time(NULL));
	for (size_t i = 0; i < size; ++i) {
		values[i] = rand() % (size / 2);
	}

	return values;
}

size_t MaxValueSimple::setTestValues(int* values, const size_t SIZE) {
	srand( time(NULL));
	size_t max_pos = rand() % SIZE;

	int max_val = 9;
	const size_t MAGNITUTE = ceil(log10(SIZE));
	for (size_t i = 0; i < MAGNITUTE; i++)
		max_val = max_val * 10 + 9;

	values[max_pos] = max_val;
	return max_pos;
}

#ifdef TESTING

int main(int argc, char* argv[]) {
	MaxValueSimple maxVal(argc, argv);

	if (maxVal.LEN < 1)
	return EXIT_FAILURE;

	maxVal.initialize();

	/* TODO logging
	 Logger.logInfo(CLAZZ, "Device type: " + maxVal.TYPE);
	 Logger.logInfo(CLAZZ, "Vector size: " + maxVal.LEN);
	 */

	/* Erzeugen der Vektoren */
	int* values = maxVal.prepareData(maxVal.LEN);
	size_t max_pos = maxVal.setTestValues(values, maxVal.LEN);

	/* TODO Logging
	 Logger.logDebug(CLAZZ,
	 "max_pos: " + max_pos + "; values[max_pos]: " + values[max_pos]);
	 */
	std::cout << "max_pos: " << max_pos << "; values[max_pos]: " << values[max_pos] << std::endl;

	/* Implementierung waehlen */
	int max = maxVal.maxValue(values, maxVal.LEN);

	maxVal.finalize();

	if (max > MaxValueSimple::MAX_FAILURE) {
		for (size_t i = 0; i < 5; i++)
		std::cout << "values[" << i << "]: " << values[i] << std::endl;
		return EXIT_SUCCESS;
	} else {
		// TODO Logger.logError(CLAZZ, "Error, no result!");
		return EXIT_FAILURE;
	}
}

#endif /* TESTING */
