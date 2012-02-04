Implementation of a numerical integration using trapezoidal rule.

Mapper:
* reads input data
* transforms text to IIntervalNamed (identifier, interval)

Suffle:
* groups all intervals with same identifier

Reduce:
* calculates on CPU or GPU
* integrals with same identifier are added to one sum

Folders:
* ant: ant scripts to pack JARs from binaries
* runtime: scripts for automated time measurements
* src: code for hadoop and java implementation
* test: JUnit tests

Note:
The calculation of the integrals in map phase and summation in reduce phase is a better choice.
But in this example job the GPU computation shall take place in reduce phase to investigate both phases (map phase was used in k-Means). 

Required libraries:
* Hadoop v0.20.2
* JavaCL JNA-1.0.0-RC1
* LightLogger v0.1
* StopWatch v0.1

Required projects:
* opencl_utils