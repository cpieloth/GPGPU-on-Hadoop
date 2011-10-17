#!/usr/bin/env sh
# userlogs ausmisten
PREFIX=KMeans_	# TODO to set
cat $PREFIX* | grep "JobIDs" > JobIDs
sed -i 's/.*\: //g' JobIDs	# replace prefix
sed -i 's/,/\n/g' JobIDs	# replace ,
rm -rf new
mkdir new
while read line; do cp -R userlogs/*$line* new; done < JobIDs

