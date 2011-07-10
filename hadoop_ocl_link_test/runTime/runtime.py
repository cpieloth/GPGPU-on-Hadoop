#!/usr/bin/env python
# Python 3
import shlex
import subprocess
import re
import sys
import time

# read command line arguments
if len(sys.argv) < 3:
	print('Usage: <program> <outputfile> <value name>')
	sys.exit(1)

# Variables
PRG_NAME = sys.argv[1]
DATA_NAME = sys.argv[2]
VAL_NAME = sys.argv[3]
RUNS = 5	# TODO to set
SLEEP = 2

# Print information
print('Program:', PRG_NAME)
print('Run size:', RUNS)
print('Outputfile: ', DATA_NAME, sep='', end='\n')

# Open file
file = open(DATA_NAME, 'a')

# Run tests
print('Start:')
regEx = re.compile('.*time=(.*);.*')

# prepare command to start
command = PRG_NAME	# TODO to set
print('   command:', command, end=' ')
args = shlex.split(command)
avgTime = 0

for run in range(0, RUNS):
	p = subprocess.Popen(args, stdout=subprocess.PIPE)
	p.wait()
	t = regEx.match(str(p.stdout.read()))
	avgTime = avgTime + float(t.group(1))
	print('.', end='')
	time.sleep(SLEEP)
	
avgTime = avgTime/RUNS
print('done! Average time:', avgTime)
file.write(VAL_NAME + "\t" + str(avgTime) + '\n') # TODO to set
	
# Close file
file.close()
