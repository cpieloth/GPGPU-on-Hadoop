CXXFLAGS=-O2 -Wall

INCLUDES=
LIBRARIES=

OBJECTS=

all: $(OBJECTS) DataSet DataSet.o

%.o: %.cpp
	$(CXX) $(CXXFLAGS) $(DEFINES) $(INCLUDES) -c $<
	
%: %.o
	$(CXX) $(CXXFLAGS) $< $(OBJECTS) $(LIBRARIES) -o $@
	
clean:
	$(RM) *.o DataSet
