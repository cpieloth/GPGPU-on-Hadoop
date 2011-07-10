#!/usr/bin/env sh
make -f Makefile_maxTemp clean
make -f Makefile_maxTemp

HADOOP_HOME="/opt/hadoop"

MAPPER="MaxTemperature_Map"
REDUCER="MaxTemperature_Reduce"
KERNEL="kernel.cl"

EXEC_LOCAL="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/streaming"
EXEC_DFS="/exec"

INPUT_LOCAL="${HOME}/Documents/gsod_large"
INPUT_DFS="/input"

OUTPUT_LOCAL="${HOME}/Documents/output_streaming"
OUTPUT_DFS="/output_streaming"

STREAMING="${HADOOP_HOME}/contrib/streaming/hadoop-*-streaming.jar"
#ARGS="stream -input ${INPUT_LOCAL} -file ${EXEC_LOCAL}/${KERNEL} -file ${EXEC_LOCAL}/${MAPPER} -mapper ${MAPPER} -file ${EXEC_LOCAL}/${REDUCER} -reducer ${REDUCER} -output ${OUTPUT_LOCAL}"
ARGS="stream -input ${INPUT_DFS} -file ${EXEC_LOCAL}/${KERNEL} -file ${EXEC_LOCAL}/${MAPPER} -mapper ${MAPPER} -file ${EXEC_LOCAL}/${REDUCER} -reducer ${REDUCER} -output ${OUTPUT_DFS}"

# echo $HADOOP_HOME
# echo $EXEC_HOME
# echo $MAPPER
# echo $REDUCER
# echo $ARGS

cd $EXEC_LOCAL
cp ../${KERNEL} $KERNEL
rm -f -r $OUTPUT_LOCAL

cd $HADOOP_HOME

bin/start-all.sh

bin/hadoop fs -rmr $INPUT_DFS
bin/hadoop fs -rmr $OUTPUT_DFS

bin/hadoop fs -put $INPUT_LOCAL $INPUT_DFS

bin/hadoop jar $STREAMING $ARGS

echo "Output:"
bin/hadoop dfs -cat ${OUTPUT_DFS}/part*

rm ${EXEC_LOCAL}/${KERNEL}
