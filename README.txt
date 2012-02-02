Project name:	GPGPU on Hadoop
Master's thesis:	"GPU-based acceleration of MapReduce by taking the example of OpenCL and Hadoop"
Website:	http://christof.pieloth.org/blog/masterarbeit
License: Apache License Version 2.0
Description:	 
This code does not provide a ready-to-use OpenCL support for Hadoop. The code is part of a master's thesis, which investigates the possibilities of a GPU-based acceleration of Hadoop. It tests different libraries for linking OpenCL to MapReduce framework (Hadoop streaming, Hadoop Pipes, JavaCL, JOCL). In addition, the GPU-based acceleration of the map phase and reduce phase is analysed. Furthermore, the impact of memory organization (Hadoop <> OpenCL) is investigated. For each study, there is a sub-project with the necessary code.

Used libraries & software:
* JavaCL (JNA-1.0.0-RC1) - http://code.google.com/p/javacl/
* JOCL (v0.1.6) - http://www.jocl.org/
* AMD Aparapi - http://developer.amd.com/zones/java/aparapi/
* Hadoop (v0.20.2) - http://hadoop.apache.org/
* LightLogger (v0.1) - https://github.com/executor85/lightLogger_Java
* StopWatch (v0.1) - https://github.com/executor85/JavaUtils
* Installed OpenCL driver (OpenCL Version 1.0)
