#!/usr/bin/env sh
make clean
make

# Hadoop Pipes require HDFS

HADOOP_HOME="/opt/hadoop"
EXEC_LOCAL="/Users/christof/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/pipes/mrSimple"
EXEC_DFS="/exec/mrSimple"
INPUT_LOCAL="${HADOOP_HOME}/input"
INPUT_DFS="/input"
OUTPUT_LOCAL="${HADOOP_HOME}/output_pipes"
OUTPUT_DFS="/output_pipes"
ARGS="-input $INPUT_DFS -output $OUTPUT_DFS -program $EXEC_DFS"
OPTS="-D hadoop.pipes.java.recordreader=true -D hadoop.pipes.java.recordwriter=true"

cd $HADOOP_HOME

bin/start-all.sh

bin/hadoop dfs -rmr $EXEC_DFS
bin/hadoop dfs -rmr $INPUT_DFS
bin/hadoop dfs -rmr $OUTPUT_DFS

bin/hadoop fs -put $EXEC_LOCAL $EXEC_DFS
bin/hadoop fs -put $INPUT_LOCAL $INPUT_DFS

bin/hadoop pipes $OPTS $ARGS

echo "Output:"
bin/hadoop dfs -cat ${OUTPUT_DFS}/part*