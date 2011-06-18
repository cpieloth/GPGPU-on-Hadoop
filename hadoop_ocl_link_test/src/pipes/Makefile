CXXFLAGS=-O2 -Wall

HADOOP_HOME=/usr/local/hadoop

# PLATFORM=$(shell uname -a)
# PLATFORM=Linux-i386-32
# PLATFORM=Linux-amd64-64
# INCLUDES=-I$(HADOOP_HOME)/c++/$(PLATFORM)/include
# LIBRARIES=-L$(HADOOP_HOME)/c++/$(PLATFORM)/lib -lhadooppipes -lhadooputils -lpthread

# FOR MAC, SEE HPIPES_ON_MAC.txt
INCLUDES=-I$(HADOOP_HOME)/src/c++/install/include
LIBRARIES=-L$(HADOOP_HOME)/src/c++/install/lib -lhadooppipes -lhadooputils -lpthread

all: mrSimple

%: %.cpp
	$(CXX) $(CXXFLAGS) $(INCLUDES) $(LIBRARIES) -o $@ $<
	
clean:
	$(RM) *.o mrSimple