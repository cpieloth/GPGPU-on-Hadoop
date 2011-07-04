#ifndef KERNEL_PATH
#define KERNEL_PATH "../kernel.cl"
#endif

#ifndef __CL_ENABLE_EXCEPTIONS
#define __CL_ENABLE_EXCEPTIONS
#endif

#include "CL/cl.hpp"

#include <limits>

class MaxValueSimple {
public:
	MaxValueSimple(int argc, char** argv);
	MaxValueSimple();
	bool initialize();
	bool initialize(cl_device_type type);
	void finalize();
	int maxValue(int values[]);
	/* Kommandozeilenargumente */
	static const char CPU = 's';
	static const char CLCPU = 'c';
	static const char CLGPU = 'g';

	static const char INFO = 'i';
	static const char ERROR = 'e';
	static const char DEBUG = 'd';

	/* Globale Variablen */
	int LEN = -1;
	char TYPE;
	static const int MAX_FAILURE = std::numeric_limits<int>::min();

private:
	int maxValue(cl_device_type type, int values[]);
	int calcWorkGroupSize(int globalSize, const long MAX_GROUP_SIZE);
	std::string readFile(std::string file);
	bool checkArguments(int argc, char** argv);
	// int[] prepareData(int size);
	// int setTestValues(int[] values);

	static const int WG_FAC = 64;
	cl_platform_id platforms[];
	cl_device_id devices[];
	cl_context context;
	cl_command_queue cmdQ[];
	cl_program program;
	cl_kernel kernels[];
	cl_event events[];
	int numDevices;
};

// TODO implement MaxValueSimple

#ifdef TESTING

int main(int argc, char* argv[]) {
	// TODO
	return 0;
}

#endif /* TESTING */
