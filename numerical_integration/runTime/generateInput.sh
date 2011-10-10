#!/usr/bin/env sh
OUTPUT_PATH=$HOME"/Documents/ni_data"
OUTPUT_FILE="intervals"
INTERVALS=("50" "250" "500" "750" "1000")
JAVA="java -jar"
JAVA_PROG=$HOME"/Dropbox/GPGPU-on-Hadoop/Jars/NumericalIntegration/NIData.jar"
START=-100
END=100

for ints in ${INTERVALS[@]}; do
	# concat file name
	output=$OUTPUT_PATH"/"$OUTPUT_FILE"_"$ints
	echo "Generating $ints intervals ..."
	# run the generator
	$JAVA $JAVA_PROG $output $START $END $ints $OUTPUT_FILE"_"$ints
done
