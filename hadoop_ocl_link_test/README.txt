Test of different libraries for linking OpenCL to MapReduce framework (Hadoop Streaming, Hadoop Pipes, JavaCL, JOCL).
Using an example job to extract the maximum temperature of a year.

Mapper:
* reads input data
* extracts the year and temperature of the record

Shuffle:
* groups all temperatures of the same year

Reducer:
* pics the maximum temperature (CPU or GPU)

Folders:
* runtime: scripts for automated time measurements
* src: code for hadoop and java implementation

Required libraries:
* Hadoop v0.20.2
* JavaCL JNA-1.0.0-RC1
* JOCL v0.1.6
* LightLogger v0.1