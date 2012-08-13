Master's thesis:	"GPU-based acceleration of MapReduce by taking the example of OpenCL and Hadoop"
Project name:	GPGPU on Hadoop
License:	Apache License Version 2.0
Example jobs:	k-Means Clustering, Numerical Integration
Description:	 
This code does not provide a ready-to-use OpenCL support for Hadoop. The code is part of a master's thesis, which investigates the possibilities of a GPU-based acceleration of Hadoop.
It tests different libraries for linking OpenCL to MapReduce framework (Hadoop Streaming, Hadoop Pipes, JavaCL, JOCL). In addition, the GPU-based acceleration of the map phase and reduce phase is analysed. Furthermore, the impact of memory organization (Hadoop <> OpenCL) is investigated. For each study, there is a sub-project with the necessary code.

Further investigations & TODOs:
Many interesting aspects have arose from the thesis. At the moment I do not have time for further investigations.
But I would appreciate it and offer support if someone wants to investigate these and other aspects:
* sophisticated and universal memory organisation
** buffered/block transfers instead of single Key/Value transfers
** reuse of Java storage objects to reduce time spend in Garbage Collector
** usage of more device memory, even if JVM memory is smaller than device memory
** Writable interface could be reused for serialization between Java and OpenCL device
* GPGPU support for TaskTracker (move GPGPU support to Hadoop core)
** denote a job and its tasks as a GPGPU job
** GPU is processing many atomic tasks in parallel and not one task in parallel
** launch only a single kernel for each jop
** async data transfer while kernel is running, see CUDA Streams
* support of Hadoop 1.x.y
* summarize thesis to a short paper in english language
* collect information about GPGPU on  Hadoop investigations on Hadoop's wiki

Used libraries & software:
* JavaCL (JNA-1.0.0-RC1) - http://code.google.com/p/javacl/
* JOCL (v0.1.6) - http://www.jocl.org/
* AMD Aparapi - http://developer.amd.com/zones/java/aparapi/
* Hadoop (v0.20.2) - http://hadoop.apache.org/
* LightLogger (v0.1) - https://github.com/cpieloth/lightLogger_Java
* StopWatch (v0.1) - https://github.com/cpieloth/JavaUtils
* OpenCL driver (OpenCL Version 1.0)