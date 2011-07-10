#!/usr/bin/env sh
DIR=$(dirname $0)
if [ "." = "$DIR" ]; then
	DIR=".."
else
	DIR="."
fi
source $DIR/utils.sh

MAPPER="MaxTemperature_Map"
REDUCER="MaxTemperature_Reduce"
KERNEL="kernel.cl"

# EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/streaming"
EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/streaming"

OUTPUT_LOCAL="${HOME}/Documents/output_streaming"
OUTPUT_DFS="/output_streaming"

STREAMING="${HADOOP_HOME}/contrib/streaming/hadoop-*-streaming.jar"
ARGS="stream -input ${INPUT_DFS} -file ${EXEC_LOCAL}/${KERNEL} -file ${EXEC_LOCAL}/${MAPPER} -mapper ${MAPPER} -file ${EXEC_LOCAL}/${REDUCER} -reducer ${REDUCER} -output ${OUTPUT_DFS}"

# Prepare executables
# cd $EXEC_LOCAL
# make -f Makefile_maxTemp clean
# make -f Makefile_maxTemp
# cp ../${KERNEL} $KERNEL

# Prepare Hadoop
cd $HADOOP_HOME
rm -f -r $OUTPUT_LOCAL
bin/hadoop fs -rmr $OUTPUT_DFS

# Start job
getSeconds
START=$GETSECONDS

bin/hadoop jar $STREAMING $ARGS

getSeconds
END=$GETSECONDS

# Print time
getDiff $END $START
echo "time=$GETDIFF;"

# Clean executable
# cd $EXEC_LOCAL
# make -f Makefile_maxTemp clean
# rm $KERNEL
