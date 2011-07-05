#!/usr/bin/env sh
echo "Build binaries ..."
make -f Makefile_maxTemp clean
make -f Makefile_maxTemp
echo "   DONE!"

echo "Check input and output folder ..."
INPUT="$HOME/Dokumente/gsod_large/*.txt"
OUTPUT="$HOME/Dokumente/output_streaming"

rm -R $OUTPUT
mkdir $OUTPUT
echo "   DONE!"

echo "Start job ..."
cat $INPUT | ./MaxTemperature_Map | ./MaxTemperature_Reduce >> ${OUTPUT}/max.txt
echo "   DONE!"
