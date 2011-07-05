#!/usr/bin/env sh
make -f Makefile_maxTemp clean
make -f Makefile_maxTemp

HADOOP_HOME="/opt/hadoop"
STREAMING="${HADOOP_HOME}/contrib/streaming/hadoop-*-streaming.jar"
EXEC_HOME="${HOME}/Dokumente/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/src/streaming"
MAPPER="MaxTemperature_Map"
REDUCER="MaxTemperature_Reduce"
KERNEL="kernel.cl"
INPUT="${HOME}/Dokumente/gsod_large/"
OUTPUT="${HOME}/Dokumente/output_streaming"
ARGS="stream -input ${INPUT} -file ${EXEC_HOME}/${KERNEL} -file ${EXEC_HOME}/${MAPPER} -mapper ${MAPPER} -file ${EXEC_HOME}/${REDUCER} -reducer ${REDUCER} -output ${OUTPUT}"

# echo $HADOOP_HOME
# echo $EXEC_HOME
# echo $MAPPER
# echo $REDUCER
# echo $ARGS

cd $EXEC_HOME
cp ../${KERNEL} $KERNEL

cd $HADOOP_HOME
rm -f -r $OUTPUT
bin/hadoop jar $STREAMING $ARGS

rm ${EXEC_HOME}/${KERNEL}
