CXXFLAGS=-O2 -g -Wall -Wextra
#DEFINES=-DRUNTIME_MEASUREMENT
OBJECTS=timeUtils.o logging.o
INCLUDES=-I"$(CLSDKROOT)/include" -I"include"

all: $(OBJECTS) maxValue maxValue.o

%.o: %.cpp
	$(CXX) $(CXXFLAGS) $(DEFINES) $(INCLUDES) -c $<
	
%: %.o
	$(CXX) $(CXXFLAGS) $< $(OBJECTS) -lOpenCL -L"$(CLSDKLIB)" -o $@

clean:
	$(RM) *.o *.so *.a maxValue
