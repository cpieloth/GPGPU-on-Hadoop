Implementation of k-means clustering in a n-dimensional space.

Mapper:
* setup() reads the centroids from distributed cache to local RAM
* assigns the closest centroid to each point (CPU or GPU)

Shuffle:
* groups all points of one centroids

Reducer:
* calculates the new mean of one group (CPU)

Folders:
* ant: ant scripts to pack JARs from binaries
* runTime: scripts for automated time measurements
* src: code for hadoop and java implementation
* test: JUnit tests

Note:
* one job is one iteration of the k-means algorithm
* centroids are shared via distributed cache
* centroids must fit in RAM (CPU and GPU)!

Required libraries:
* Hadoop v0.20.2
* JavaCL JNA-1.0.0-RC1
* LightLogger v0.1
* StopWatch v0.1

Required projects:
* opencl_utils