#!/usr/bin/env sh
make clean
make

INPUT="$HOME/Dokumente/gsod_large/*.txt"
OUTPUT="$HOME/Dokumente/output_streaming"

rm -R $OUTPUT
mkdir $OUTPUT

cat $INPUT | ./MaxTemperature_Map | ./MaxTemperature_Reduce >> ${OUTPUT}/max.txt
