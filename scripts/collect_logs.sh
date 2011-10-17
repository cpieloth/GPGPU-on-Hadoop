#!/usr/bin/env sh
# TODO Für jede Eingabegröße Ordner erstellen und Dateien reinkopieren
PREFIX=KMeans_	# TODO to set
ls | grep $PREFIX > folders
while read line; do
	cd $line

	cat $PREFIX*_cpu | grep "JobIDs" > JobIDs_cpu
	sed -i 's/.*\: //g' JobIDs_cpu	# replace prefix
	sed -i 's/,/\n/g' JobIDs_cpu	# replace ,
	rm -f stdout_cpu
	while read line; do cat ../new/*$line*/stdout >> stdout_cpu; done < JobIDs_cpu

	cat $PREFIX*_ocl | grep "JobIDs" > JobIDs_ocl
	sed -i 's/.*\: //g' JobIDs_ocl	# replace prefix
	sed -i 's/,/\n/g' JobIDs_ocl	# replace ,
	rm -f stdout_ocl
	while read line; do cat ../new/*$line*/stdout >> stdout_ocl; done < JobIDs_ocl
	cd ..
done < folders
