CXXFLAGS=-O2	-Wall -Werror

all: mapSimple reduceSimple

%: %.cpp
	g++ $(CXXFLAGS) -o $@ $<
	
clean:
	$(RM) *.o mapSimple reduceSimple