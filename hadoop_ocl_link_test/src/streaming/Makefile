CXXFLAGS=-O2	-Wall -Werror

all: MaxTemperature_Map MaxTemperature_Reduce

%: %.cpp
	g++ $(CXXFLAGS) -o $@ $<
	
clean:
	$(RM) *.o MaxTemperature_Map MaxTemperature_Reduce