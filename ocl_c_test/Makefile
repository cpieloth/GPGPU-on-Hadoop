CXXFLAGS=-g -Wall -Wextra
#DEFINES=-DRUNTIME_MEASUREMENT
OBJECTS=timeUtils.o logging.o
INCLUDES=-I"$(CLSDKINCLUDE)" -I"include"
# LIBRARIES OS DEPENDENT
OSTYPE = $(shell uname)
ifeq ($(OSTYPE),Linux)
	LIBRARIES=-L$(CLSDKLIB) -lOpenCL
endif
ifeq ($(OSTYPE),Darwin)
	LIBRARIES=-framework OpenCL
endif

all: $(OBJECTS) maxValue maxValue.o

%.o: %.cpp
	$(CXX) $(CXXFLAGS) $(DEFINES) $(INCLUDES) -c $<
	
%: %.o
	$(CXX) $(CXXFLAGS) $< $(OBJECTS) $(LIBRARIES) -o $@

clean:
	$(RM) *.o *.so *.a maxValue
