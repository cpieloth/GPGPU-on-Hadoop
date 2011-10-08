#!/usr/bin/env sh
OUTPUT_PATH=$HOME"/Documents/ni_data"
OUTPUT_FILE="intervals"
INTERVAL="50"
JAVA="java -jar"
JAVA_PROG=$HOME"/Dropbox/GPGPU-on-Hadoop/Jars/NumericalIntegration/NIData.jar"

START=-100
END=-50
# concat file name
output=$OUTPUT_PATH"/"$OUTPUT_FILE"1"
echo "Generating $INTERVAL intervals with the resolution of $res ..."
# run the generator
$JAVA $JAVA_PROG $output $START $END $INTERVAL $OUTPUT_FILE"1"

START=-50
END=0
# concat file name
output=$OUTPUT_PATH"/"$OUTPUT_FILE"2"
echo "Generating $INTERVAL intervals with the resolution of $res ..."
# run the generator
$JAVA $JAVA_PROG $output $START $END $INTERVAL $OUTPUT_FILE"2"

START=0
END=50
# concat file name
output=$OUTPUT_PATH"/"$OUTPUT_FILE"3"
echo "Generating $INTERVAL intervals with the resolution of $res ..."
# run the generator
$JAVA $JAVA_PROG $output $START $END $INTERVAL $OUTPUT_FILE"3"

START=50
END=100
# concat file name
output=$OUTPUT_PATH"/"$OUTPUT_FILE"4"
echo "Generating $INTERVAL intervals with the resolution of $res ..."
# run the generator
$JAVA $JAVA_PROG $output $START $END $INTERVAL $OUTPUT_FILE"4"

