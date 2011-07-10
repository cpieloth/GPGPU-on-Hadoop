#!/usr/bin/env sh
make -f Makefile_maxTemp clean
make -f Makefile_maxTemp

# Hadoop Pipes require HDFS

HADOOP_HOME="/opt/hadoop"

MAPRED="MaxTemperature"
KERNEL="kernel.cl"

EXEC_LOCAL="$HOME/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/pipes"
EXEC_DFS="/exec"

INPUT_LOCAL="$HOME/Documents/gsod_large"
INPUT_DFS="/input"

OUTPUT_LOCAL="$HOME/Documents/output_pipes"
OUTPUT_DFS="/output_pipes"

ARGS="-input $INPUT_DFS -output $OUTPUT_DFS -program $EXEC_DFS/$MAPRED"
OPTS="-D hadoop.pipes.java.recordreader=true -D hadoop.pipes.java.recordwriter=true -files ${EXEC_LOCAL}/${KERNEL}"

cd $EXEC_LOCAL
cp ../$KERNEL $KERNEL

cd $HADOOP_HOME

bin/start-all.sh

bin/hadoop fs -rmr $EXEC_DFS/$MAPRED
bin/hadoop fs -rmr $EXEC_DFS/$KERNEL
bin/hadoop fs -rmr $INPUT_DFS
bin/hadoop fs -rmr $OUTPUT_DFS

bin/hadoop fs -put $EXEC_LOCAL/$MAPRED $EXEC_DFS/$MAPRED
bin/hadoop fs -put $EXEC_LOCAL/$KERNEL $EXEC_DFS/$KERNEL
bin/hadoop fs -put $INPUT_LOCAL $INPUT_DFS

bin/hadoop pipes $OPTS $ARGS

echo "Output:"
bin/hadoop dfs -cat ${OUTPUT_DFS}/part*

rm ${EXEC_LOCAL}/${KERNEL}
