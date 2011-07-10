#!/usr/bin/env sh
HADOOP_HOME="/opt/hadoop"
EXEC_DFS="/exec"

INPUT_LOCAL="${HOME}/Documents/gsod_large"
INPUT_DFS="/input"

### Time functions ###
# scale of 9 because nano seconds
SCALE=9	
# Return value GETSECONDS
getSeconds()
{
	NSEC=$(date +%s%N)
	SEC=$(echo "scale=$SCALE; $NSEC*(10^-9)" | bc)
	GETSECONDS=$SEC
}

# Return value GETDIFF
getDiff()
{
	DIFF=$(echo "scale=$SCALE; $1-$2" | bc)
	GETDIFF=$DIFF
}
