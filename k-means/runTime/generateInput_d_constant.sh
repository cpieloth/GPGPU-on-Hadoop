#!/usr/bin/env sh
OUTPUT_PATH="$HOME/Documents/km_data"
OUTPUT_POINT="points"
OUTPUT_CENTER="center"
DATA_SIZE=("16" "32" "64")
K_SIZE=("128" "512" "2048")
ITEM_SIZE=11
MB=1048576
DIM=64
JAVA="java -Xms1536m -Xmx2048m -jar"
JAVA_PROG="$HOME/Dropbox/GPGPU-on-Hadoop/Jars/KMeans/KMeansData.jar"

for k in ${K_SIZE[@]}; do
	for dsize in ${DATA_SIZE[@]}; do
		# calc point count
		let psize=ITEM_SIZE*DIM
		let items=dsize*MB
		let items=items/psize
		echo "Generating for $items points, $DIM dimension and $k clusters ..."
		# concat file name
		output=$OUTPUT_PATH"/"$OUTPUT_POINT"_"$dsize"mb_"$k"k"
		# run the generator for points
		$JAVA $JAVA_PROG $output $items $DIM $k
		# run the generator for center
		output=$OUTPUT_PATH"/"$OUTPUT_CENTER"_"$dsize"mb_"$k"k"
		$JAVA $JAVA_PROG $output $k $DIM 0
	done
done
