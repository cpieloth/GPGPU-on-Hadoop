OSTYPE = $(shell uname)

CXXFLAGS=-O2 -g -Wall -Werror
INCLUDES=-I"$(CLSDKINCLUDE)"
# LIBRARIES OS DEPENDENT
ifeq ($(OSTYPE),Linux)
	LIBRARIES=-L$(CLSDKLIB) -lOpenCL
endif
ifeq ($(OSTYPE),Darwin)
	LIBRARIES=-framework OpenCL
endif

all: CLInfo CLInfo.o

%: %.o
	$(CXX) $(CXXFLAGS) $< $(LIBRARIES) -o $@

%.o: %.cpp
	$(CXX) $(CXXFLAGS) $(INCLUDES) -c $<

clean:
	$(RM) *.o *.so *.a CLInfo
	
