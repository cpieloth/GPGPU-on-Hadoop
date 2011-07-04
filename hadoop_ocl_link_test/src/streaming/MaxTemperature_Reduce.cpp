#include <iostream>
#include <string>
#include <string.h>
#include <limits>
#include <algorithm>

#include "../gsod_cpp/DataSet.cpp"
#include "../maxValueCpp/MaxValueSimple.cpp"

using namespace gsod;

const int MAX_VALUES = 65536;
int buffer[MAX_VALUES];

int max;

int main() {
	// TODO
	// new MaxValueCL
	// initialize MaxValueCL

	std::string line;
	// int i;
	std::string key, key_prev, temp;
	int value;

	size_t found;
	max = std::numeric_limits<int>::min();

	while (getline(std::cin, line)) {
		// Split string
		found = line.find("\t");
		key = line.substr(0, found);
		temp = line.substr(found, line.length() - found);
		value = atoi(temp.c_str());

		if(!key_prev.empty() && key.compare(key_prev) != 0) {
			std::cout << key_prev << "\t" << max << std::endl;
			max = std::numeric_limits<int>::min();
		}

		key_prev = key;
		max = std::max(max, value);
	}

	std::cout << key_prev << "\t" << max << std::endl;

	return EXIT_SUCCESS;
}
