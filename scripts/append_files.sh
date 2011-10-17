#!/usr/bin/env sh
# TODO set
FOLDER="/home/christof/Dropbox/GPGPU-on-Hadoop/runtime/ec2/Numerical Integration/ni_xsinx-100_to_100/ni_logs_res_constant"
PREFIX=NumIntegration_
SUFFIX=_ocl

echo $FOLDER
echo $PREFIX
echo $SUFFIX

for line in $(ls "$FOLDER" | grep $PREFIX); do
	cd "$FOLDER/$line"
	cat $PREFIX*$SUFFIX >> stdout$SUFFIX	# TODO set
	cd ..
done
echo "Done!"

