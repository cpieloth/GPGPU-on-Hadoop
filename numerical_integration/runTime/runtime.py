#!/usr/bin/env python
# Python 3
import shlex
import subprocess
import re
import os
import time

# Variables
HOME = os.environ.get("HOME")
HADOOP_CMD = "/opt/hadoop/bin/hadoop"
PRG_NAME = HOME + "/Dropbox/GPGPU-on-Hadoop/Jars/NumericalIntegration/NIHadoopNamed.jar"    # TODO to set
RESOLUTIONS = ["1000", "10000", "1000000"]    # TODO to set
MODES = ["cpu", "ocl"]  # TODO to set
RUNS = 3    # TODO to set
LOG_PATH = "/tmp/ni_logs"
DATA_PATH = HOME + "/Documents/ni_data"
INPUT_FILES = ["intervals1", "intervals2", "intervals3", "intervals4"]  # TODO to set
HDFS_INPUT = "/ni_data/input"
HDFS_OUTPUT = "/ni_data/output"

# Functions
def copyHdfs():
    command = HADOOP_CMD + " fs -mkdir " + HDFS_INPUT
    args = shlex.split(command)
    p = subprocess.Popen(args, stdout=subprocess.PIPE)
    p.wait()
    for file in INPUT_FILES:
        command = HADOOP_CMD + " fs -put " + DATA_PATH + "/" + file + " " + HDFS_INPUT + "/"
        args = shlex.split(command)
        p = subprocess.Popen(args, stdout=subprocess.PIPE)
        p.wait()
    
def clearHdfs():
    command = HADOOP_CMD + " fs -rmr " + HDFS_INPUT
    args = shlex.split(command)
    p = subprocess.Popen(args, stdout=subprocess.PIPE)
    p.wait()
    command = HADOOP_CMD + " fs -rmr " + HDFS_OUTPUT
    args = shlex.split(command)
    p = subprocess.Popen(args, stdout=subprocess.PIPE)
    p.wait()
    
def clearIntermediateHdfs():
    command = HADOOP_CMD + " fs -rmr " + HDFS_OUTPUT
    args = shlex.split(command)
    p = subprocess.Popen(args, stdout=subprocess.PIPE)
    p.wait()

# Print information
print('Home:', HOME)
print('Hadoop:', HADOOP_CMD)
print('Program:', PRG_NAME)
print('Log path:', LOG_PATH)
print('HDFS input:', HDFS_INPUT)
print('HDFS output:', HDFS_OUTPUT)
print()

# Create log path
command = "mkdir " + LOG_PATH
args = shlex.split(command)
p = subprocess.Popen(args, stdout=subprocess.PIPE)
p.wait()

# Run tests
print('Start runs ...')

for res in RESOLUTIONS:
    print('    res size: ', res)
    # prepare HDFS
    print("    Clear HDFS ...")
    clearHdfs()
    print("    Clear HDFS finished!")
    print("    Copy data to HDFS ...")
    copyHdfs()
    print("    Copy data HDFS finished!")
    print("    Waiting 3 seconds for duplication!")
    time.sleep(3)
    
    for mode in MODES:
        print('    mode: ', mode)
        # prepare command to start
        jobName = "NumIntegration_" + res + "_" + mode
        command = HADOOP_CMD + " jar " + PRG_NAME + " " + jobName + " " + HDFS_INPUT + " " + HDFS_OUTPUT + " " + "xsinx" + " " + "0" + " " + res + " " + mode   # TODO to set
        print('    command: ', command)
        args = shlex.split(command)
        # TODO start job
        file = open(LOG_PATH + "/" + jobName, 'w+b')
        
        for run in range(0, RUNS):
            print("    Starting run #", run)
            # clear intermediate data
            print("    Clear intermediate HDFS data ...")
            clearIntermediateHdfs()
            print("    Clear intermediate HDFS data finished!")
            # run
            p = subprocess.Popen(args, stdout=subprocess.PIPE)
            p.wait()
            
            file.write(p.stdout.read())
            print("    Finished run #", run)
            
        file.close()
        print()
        
    print()
    
print()
    
print('finished runs!') 
