#!/usr/bin/env sh
source utils.sh

PAUSE="sleep 2"

# Input data
INPUT_DFS="/input"
INPUT_SMALL="$HOME/Documents/gsod_small"
INPUT_MEDIUM="$HOME/Documents/gsod_medium"
INPUT_LARGE="$HOME/Documents/gsod_large"

# Output data
SUFFIX=".dat"
HADOOP_DAT="hadoop$SUFFIX"
JCL_DAT="javacl$SUFFIX"
JOCL_DAT="jocl$SUFFIX"
STREAMING_DAT="streaming$SUFFIX"
PIPES_DAT="pipes$SUFFIX"

rm -f $HADOOP_DAT
rm -f $JCL_DAT
rm -f $JOCL_DAT
rm -f $STREAMING_DAT
rm -f $PIPES_DAT

# Executables
HADOOP="hadoop/runHadoop.sh"
JCL="javacl/runJavaCL.sh"
JOCL="jocl/runJOCL.sh"
STREAMING="streaming/runStreaming.sh"
PIPES="pipes/runPipes.sh"
RUNTIME="runtime.py"

# Start Hadoop
$HADOOP_HOME/bin/start-all.sh
$PAUSE


# Run executables
runExecutables()
{
	python3 $RUNTIME $HADOOP $HADOOP_DAT $VAL_NAME
	$PAUSE
	python3 $RUNTIME $JCL $JCL_DAT $VAL_NAME
	$PAUSE
	python3 $RUNTIME $JOCL $JOCL_DAT $VAL_NAME
	$PAUSE
	#python3 $RUNTIME $STREAMING $STREAMING_DAT $VAL_NAME
	$PAUSE
	#python3 $RUNTIME $PIPES $PIPES_DAT $VAL_NAME
}


# Run small
echo "Start small dataset ..."
$HADOOP_HOME/bin/hadoop fs -rmr $INPUT_DFS
$PAUSE
$HADOOP_HOME/bin/hadoop fs -put $INPUT_SMALL $INPUT_DFS
$PAUSE
VAL_NAME="64"
runExecutables
$PAUSE
echo "Small dataset ... done!"


# Run medium
echo "Start medium dataset ..."
$HADOOP_HOME/bin/hadoop fs -rmr $INPUT_DFS
$PAUSE
$HADOOP_HOME/bin/hadoop fs -put $INPUT_MEDIUM $INPUT_DFS
$PAUSE
VAL_NAME="128"
runExecutables
$PAUSE
echo "Medium dataset ... done!"


# Run large
echo "Start large dataset ..."
$HADOOP_HOME/bin/hadoop fs -rmr $INPUT_DFS
$PAUSE
$HADOOP_HOME/bin/hadoop fs -put $INPUT_LARGE $INPUT_DFS
$PAUSE
VAL_NAME="256"
runExecutables
$PAUSE
echo "Large dataset ... done!"


# plot
gnuplot maxTemp_time.gnu

# Stop Hadoop
# $HADOOP_HOME/bin/stop-all.sh



