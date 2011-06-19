#!/usr/bin/env sh
make clean
make

HADOOP_HOME="/opt/hadoop"
EXEC_HOME="/home/christof/Dokumente/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/pipes"
ARGS="-input ${HADOOP_HOME}/input/vectors.txt -output ${HADOOP_HOME}/output -program ${EXEC_HOME}/mrSimple"

# echo $HADOOP_HOME
# echo $EXEC_HOME
# echo $ARGS

cd $HADOOP_HOME
rm -f -r output
bin/hadoop pipes $ARGS
