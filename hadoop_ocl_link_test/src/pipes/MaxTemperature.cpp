#include <string>

#include  <stdint.h>  // <-- this is missing from the book
#include <sstream>
#include <limits>
#include <exception>

#include "hadoop/Pipes.hh"
#include "hadoop/TemplateFactory.hh"

#include "../gsod_cpp/DataSet.cpp"
#include "../maxValueCpp/MaxValueSimple.cpp"

class MaxTemp_Mapper: public HadoopPipes::Mapper {
public:

	MaxTemp_Mapper(HadoopPipes::TaskContext& context) {
	}

	void map(HadoopPipes::MapContext& context) {
		line = context.getInputValue();

		size_t found = line.find_first_of("STN---");
		if (found != std::string::npos)
			return;

		year = DataSet::getYear(line);
		airTemperature = DataSet::getMax(line);

		if (airTemperature != DataSet::MISSING) {
			valStr.str("");
			valStr << airTemperature;
			context.emit(year, valStr.str());
		}

	}

private:
	std::string line;
	std::string year;
	int airTemperature;
	std::stringstream valStr;
};

class MaxTemp_Reducer: public HadoopPipes::Reducer {
public:

	MaxTemp_Reducer(HadoopPipes::TaskContext& context) {
		maxVal = MaxValueSimple();
		if (!maxVal.initialize(CL_DEVICE_TYPE_GPU))
			throw std::exception();

		buffer = (int*) calloc(MaxTemp_Reducer::MAX_VALUES, sizeof(int));
	}

	~MaxTemp_Reducer() {
		maxVal.finalize();
		free(buffer);
	}

	void reduce(HadoopPipes::ReduceContext& context) {
		buffer[0] = MaxValueSimple::MAX_FAILURE;
		i = 1;

		while (context.nextValue()) {
			if (i < MaxTemp_Reducer::MAX_VALUES)
				buffer[i++] = atoi(context.getInputValue().c_str());
			if (i >= MaxTemp_Reducer::MAX_VALUES) {
				buffer[0]
						= maxVal.maxValue(buffer, MaxTemp_Reducer::MAX_VALUES);
				i = 1;
			}
		}
		// work-around for context/iterable.hasNext()
		if (i > 1)
			buffer[0] = maxVal.maxValue(buffer, MaxTemp_Reducer::MAX_VALUES);

		valStr.str("");
		valStr << buffer[0];
		context.emit(context.getInputKey(), valStr.str());
	}

private:
	size_t i;
	std::stringstream valStr;
	static const size_t MAX_VALUES = 65536;
	int* buffer;
	MaxValueSimple maxVal;
};

int main(int argc, char* argv[]) {
	return HadoopPipes::runTask(
			HadoopPipes::TemplateFactory<MaxTemp_Mapper, MaxTemp_Reducer>());
}
