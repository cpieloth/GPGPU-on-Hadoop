#include <string>
#include <iostream>
#include <stdlib.h>

namespace gsod {

class DataSet {
public:
	static const int YEAR_START = 14;
	static const int YEAR_DIFF = 4;

	static const int TEMP_START = 25;
	static const int TEMP_DIFF = 6;

	static const int MAX_START = 103;
	static const int MAX_DIFF = 6;

	static const int MISSING = 9999;

	static std::string getYear(std::string line);

	static int getTemp(std::string line);

	static int getMax(std::string line);

private:
	static int getInt(std::string line, const int START, const int DIFF);

};

}

using namespace gsod;

std::string DataSet::getYear(std::string line) {
	return line.substr(YEAR_START, YEAR_DIFF);
}

int DataSet::getTemp(std::string line) {
	return getInt(line, DataSet::TEMP_START, DataSet::TEMP_DIFF);
}

int DataSet::getMax(std::string line) {
	return getInt(line, DataSet::MAX_START, DataSet::MAX_DIFF);
}

int DataSet::getInt(std::string line, const int START, const int DIFF) {
	return (int) (strtod(line.substr(START, DIFF).data(), NULL) * 10);
}

#ifdef TESTING

int main(int argc, char* argv[]) {
	std::string line("654420 99999  20070703    73.0  5    70.6  5  9999.9  0  9999.9  0    6.4  5    3.8  5    8.0  999.9    77.0*   71.6*  0.04A 999.9  010000");
	std::cout << line << std::endl;

	std::cout << "Year: " << DataSet::getYear(line) << std::endl;
	std::cout << "Temp: " << DataSet::getTemp(line) << std::endl;
	std::cout << "Max: " << DataSet::getMax(line) << std::endl;

	return 0;
}

#endif /* TESTING */

