#!/usr/bin/env sh
OUTPUT_PATH="/Users/christof/Documents/ni_data"
OUTPUT_FILE="intervals"
RESOLUTION=("1000" "10000" "100000" "1000000")
INTERVALS=("10" "100" "1000")
JAVA="java -jar"
JAVA_PROG="/Users/christof/Dropbox/GPGPU-on-Hadoop/Jars/NumericalIntegration/NIData.jar"
START=-100
END=100

for res in ${RESOLUTION[@]}; do
	for int in ${INTERVALS[@]}; do
		# concat file name
		output=$OUTPUT_PATH"/"$OUTPUT_FILE"_"$res"res_"$int"ints"
		echo "Generating $int intervals with the resolution of $res ..."
		# run the generator
		$JAVA $JAVA_PROG $output $START $END $int $res "lfs"
	done
done