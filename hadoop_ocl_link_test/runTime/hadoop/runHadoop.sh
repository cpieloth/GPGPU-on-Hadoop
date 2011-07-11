#!/usr/bin/env sh
UTIL_HOME="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/"
source $UTIL_HOME/utils.sh

MAPRED="maxTemp.jar"

# EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/hadoop"
EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/hadoop"

OUTPUT_LOCAL="${HOME}/Documents/output"
OUTPUT_DFS="/output_hadoop"

ARGS="maxTemp $INPUT_DFS $OUTPUT_DFS"

# Prepare executables
# cd $EXEC_LOCAL

# Prepare Hadoop
cd $HADOOP_HOME
rm -f -r $OUTPUT_LOCAL
bin/hadoop fs -rmr $OUTPUT_DFS

# Start job
getSeconds
START=$GETSECONDS

bin/hadoop jar $EXEC_LOCAL/$MAPRED $ARGS

getSeconds
END=$GETSECONDS

# Print time
getDiff $END $START
echo "time=$GETDIFF;"

# Clean executable
# cd $EXEC_LOCAL
