#include <iostream>
#include <string>
#include <string.h>
#include <limits>
#include <algorithm>

#include "../gsod_cpp/DataSet.cpp"
#include "../maxValueCpp/MaxValueSimple.cpp"

using namespace gsod;

int* buffer;
const size_t MAX_VALUES = 65536;

int main() {
	MaxValueSimple maxVal;
	maxVal = MaxValueSimple();
	if(!maxVal.initialize(CL_DEVICE_TYPE_GPU))
		return EXIT_FAILURE;
	buffer = (int*)calloc(MAX_VALUES, sizeof(int));

	std::string line;
	std::string key, key_prev, valStr;
	int value;

	size_t found;
	size_t i = 1;
	buffer[0] = MaxValueSimple::MAX_FAILURE;

	while (getline(std::cin, line)) {
		// Split string
		found = line.find("\t");
		key = line.substr(0, found);
		valStr = line.substr(found, line.length() - found);
		value = atoi(valStr.c_str());

		// new key ... new reduce task
		if (!key_prev.empty() && key.compare(key_prev) != 0) {
			buffer[0] = maxVal.maxValue(buffer, MAX_VALUES);
			std::cout << key_prev << "\t" << buffer[0] << std::endl;
			// reset data and counters
			i = 1;
			free(buffer);
			buffer = (int*)calloc(MAX_VALUES, sizeof(int));
			buffer[0] = MaxValueSimple::MAX_FAILURE;
		}

		key_prev = key;
		if (i < MAX_VALUES) {
			buffer[i++] = value;
		}
		// if max values or no more values to add, start opencl kernel
		if (i >= MAX_VALUES) {
			buffer[0] = maxVal.maxValue(buffer, MAX_VALUES);
			i = 1;
		}
	}

	// work-around for context/iterable.hasNext()
	if(i > 0)
		buffer[0] = maxVal.maxValue(buffer, MAX_VALUES);

	std::cout << key_prev << "\t" << buffer[0] << std::endl;

	maxVal.finalize();
	free(buffer);

	return EXIT_SUCCESS;
}
