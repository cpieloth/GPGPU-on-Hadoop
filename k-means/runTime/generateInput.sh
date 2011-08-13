#!/usr/bin/env sh
OUTPUT_PATH="/Users/christof/Documents/km_data"
OUTPUT_FILE="points"
DATA_SIZE=("128" "256" "512")
DIM_SIZE=("2" "64" "256")
ITEM_SIZE=11
MB=1048576
K_FACTOR=10
JAVA="java -Xms2048m -Xmx2560m -jar"
JAVA_PROG="/Users/christof/Dropbox/GPGPU-on-Hadoop/Jars/KMeans/KMeansData.jar"

for dim in ${DIM_SIZE[@]}; do
	for dsize in ${DATA_SIZE[@]}; do
		# calc point count
		let psize=ITEM_SIZE*dim
		let items=dsize*MB
		let items=items/psize
		# concat file name
		output=$OUTPUT_PATH"/"$OUTPUT_FILE"_"$dim"d_"$dsize"mb"
		let clusters=dim*K_FACTOR
		echo "Generating for $items points, $dim dimension and $clusters clusters ..."
		# run the generator
		$JAVA $JAVA_PROG $output $items $dim $clusters "lfs"
	done
done