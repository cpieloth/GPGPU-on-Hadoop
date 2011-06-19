CXXFLAGS=-O2 -Wall

INCLUDES=
LIBRARIES=

HADOOP_HOME=/opt/hadoop
OSTYPE=$(shell uname)
PLATFORM=
ifeq ($(OSTYPE),Linux)
	ARCH=$(shell uname -m)
	ifeq ($(ARCH),x86_64)
		PLATFORM=$(OSTYPE)-amd64-64
	else
		PLATFORM=$(OSTYPE)-i386-32
	endif
	INCLUDES=-I$(HADOOP_HOME)/c++/$(PLATFORM)/include
	LIBRARIES=-L$(HADOOP_HOME)/c++/$(PLATFORM)/lib -lhadooppipes -lhadooputils -lpthread -lssl
endif
# FOR MAC, SEE HPIPES_ON_MAC.txt
ifeq ($(OSTYPE),Darwin)
	INCLUDES=-I$(HADOOP_HOME)/src/c++/install/include
	LIBRARIES=-L$(HADOOP_HOME)/src/c++/install/lib -lhadooppipes -lhadooputils -lpthread -lssl
endif

all: mrSimple

%: %.cpp
	$(CXX) $(CXXFLAGS) $(INCLUDES) $< $(LIBRARIES) -o $@
	
clean:
	$(RM) *.o mrSimple