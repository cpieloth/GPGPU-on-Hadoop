#include <iostream>
#include <string>

#include "../gsod_cpp/DataSet.cpp"

using namespace gsod;

int main() {
	std::string line;
	std::string year;
	int airTemperature;

	while (getline(std::cin, line)) {

		size_t found = line.find_first_of("STN---");
		if (found != std::string::npos)
			continue;

		year = DataSet::getYear(line);
		airTemperature = DataSet::getMax(line);

		if (airTemperature != DataSet::MISSING) {
			std::cout << year << "\t" << airTemperature << std::endl;
		}

	}
	return EXIT_SUCCESS;
}
