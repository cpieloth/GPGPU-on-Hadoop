#!/usr/bin/env sh
UTIL_HOME="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/"
source $UTIL_HOME/utils.sh

MAPRED="maxTemp.jar"
KERNEL="kernel.cl"

# EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/jocl"
EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/jocl"

LIB_PATH="${HOME}/Dropbox/GPGPU-on-Hadoop/Libraries"
LIB1="JOCL-0.1.6.jar"
LIB2="lightLogger-0.1.jar"

OUTPUT_LOCAL="${HOME}/Documents/output"
OUTPUT_DFS="/output_jocl"

ARGS="maxTemp $INPUT_DFS $OUTPUT_DFS"

# Prepare executables
# cd $EXEC_LOCAL
cp $LIB_PATH/$LIB1 $HADOOP_HOME/lib/$LIB1
cp $LIB_PATH/$LIB2 $HADOOP_HOME/lib/$LIB2

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
rm -f $HADOOP_HOME/lib/$LIB1
rm -f $HADOOP_HOME/lib/$LIB2
