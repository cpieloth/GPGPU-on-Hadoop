#!/usr/bin/env sh
UTIL_HOME="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/"
source $UTIL_HOME/utils.sh

# Hadoop Pipes require HDFS
MAPRED="MaxTemperature"
KERNEL="kernel.cl"

# EXEC_LOCAL="$HOME/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/pipes"
EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/pipes"

OUTPUT_LOCAL="$HOME/Documents/output_pipes"
OUTPUT_DFS="/output_pipes"

ARGS="-input $INPUT_DFS -output $OUTPUT_DFS -program $EXEC_DFS/$MAPRED"
OPTS="-D hadoop.pipes.java.recordreader=true -D hadoop.pipes.java.recordwriter=true -files ${EXEC_LOCAL}/${KERNEL}"

# Prepare executables
# cd $EXEC_LOCAL
# make -f Makefile_maxTemp clean
# make -f Makefile_maxTemp
# cp ../$KERNEL $KERNEL

# Prepare Hadoop
cd $HADOOP_HOME
bin/hadoop fs -rmr $EXEC_DFS/$MAPRED
bin/hadoop fs -rmr $EXEC_DFS/$KERNEL
bin/hadoop fs -rmr $OUTPUT_DFS

bin/hadoop fs -put $EXEC_LOCAL/$MAPRED $EXEC_DFS/$MAPRED
bin/hadoop fs -put $EXEC_LOCAL/$KERNEL $EXEC_DFS/$KERNEL

# Start job
getSeconds
START=$GETSECONDS

bin/hadoop pipes $OPTS $ARGS

getSeconds
END=$GETSECONDS

# Print time
getDiff $END $START
echo "time=$GETDIFF;"

# Clean executable
# cd $EXEC_LOCAL
# make -f Makefile_maxTemp clean
# rm $KERNEL
