#!/usr/bin/env sh
make -f Makefile_simple clean
make -f Makefile_simple

HADOOP_HOME="/usr/local/hadoop"
STREAMING="${HADOOP_HOME}/contrib/streaming/hadoop-streaming-*.jar"
EXEC_HOME="/Users/christof/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/streaming"
MAPPER="mapSimple"
REDUCER="reduceSimple"
ARGS="stream -input ${HADOOP_HOME}/input/vectors.txt -file ${EXEC_HOME}/${MAPPER} -mapper mapSimple -file ${EXEC_HOME}/${REDUCER} -reducer reduce -output ${HADOOP_HOME}/output"

# echo $HADOOP_HOME
# echo $EXEC_HOME
# echo $MAPPER
# echo $REDUCER
# echo $ARGS

cd $HADOOP_HOME
rm -f -r output
bin/hadoop jar $STREAMING $ARGS
