#!/usr/bin/env sh
UTIL_HOME="${HOME}/Documents/GPGPU-on-Hadoop/github/hadoop_ocl_link_test/runTime/"

HADOOP_HOME="/opt/hadoop"
EXEC_DFS="/exec"

INPUT_DFS="/input"

### Time functions ###
# MAC OS workaround for floating point seconds
PY="python3 $UTIL_HOME/secondsFloat.py"
# scale of 9 because nano seconds
# SCALE=9	
# Return value GETSECONDS
getSeconds()
{
	#NSEC=$(date +%s%N)
	#SEC=$(echo "scale=$SCALE; $NSEC*(10^-9)" | bc)
	#GETSECONDS=$SEC
	GETSECONDS=$($PY "get")
}

# Return value GETDIFF
getDiff()
{
	#DIFF=$(echo "scale=$SCALE; $1-$2" | bc)
	#GETDIFF=$DIFF
	GETDIFF=$($PY "diff" $1 $2)
}