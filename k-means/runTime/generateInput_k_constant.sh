#!/usr/bin/env sh
OUTPUT_PATH="$HOME/Documents/km_data"
OUTPUT_POINT="points"
OUTPUT_CENTER="center"
DATA_SIZE=("16" "32" "64")
DIM_SIZE=("2" "64" "256")
ITEM_SIZE=11
MB=1048576
K_VALUE=256
JAVA="java -Xms1536m -Xmx2048m -jar"
JAVA_PROG="$HOME/Dropbox/GPGPU-on-Hadoop/Jars/KMeans/KMeansData.jar"

for dim in ${DIM_SIZE[@]}; do
	for dsize in ${DATA_SIZE[@]}; do
		# calc point count
		let psize=ITEM_SIZE*dim
		let items=dsize*MB
		let items=items/psize
		echo "Generating for $items points, $dim dimension and $K_VALUE clusters ..."
		# concat file name
		output=$OUTPUT_PATH"/"$OUTPUT_POINT"_"$dsize"mb_"$dim"d"
		# run the generator for points
		$JAVA $JAVA_PROG $output $items $dim $K_VALUE
		# run the generator for center
		output=$OUTPUT_PATH"/"$OUTPUT_CENTER"_"$dsize"mb_"$dim"d"
		$JAVA $JAVA_PROG $output $K_VALUE $dim 0
	done
done
