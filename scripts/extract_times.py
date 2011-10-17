#!/usr/bin/env python
# Python 3
import re

def getSize(value):
    reMapMethod = re.compile('.*NumIntegration_(.*)r_.*')
    match = reMapMethod.match(value)
    return match.group(1)

# Constant Variables - TODO set
HOME = "/home/christof/Dropbox/GPGPU-on-Hadoop/runtime/ec2/Numerical Integration/ni_xsinx-100_to_100/ni_logs_ints_constant"
IFOLDERS = ["NumIntegration_1000r_1000ic", "NumIntegration_10000r_1000ic", "NumIntegration_100000r_1000ic", "NumIntegration_1000000r_1000ic"]
IFNAME = "stdout_cpu"
OFNAME = "times_1000ic_cpu.csv"

# prepare regex - TODO set
reMapMethod = re.compile('.*mapMethodTime=(.*);.*')
reMapPhase = re.compile('.*mapPhaseTime=(.*);.*')
reReduceMethod = re.compile('.*reduceMethodTime=(.*);.*')
reReducePhase = re.compile('.*reducePhaseTime=(.*);.*')
reTotalTime = re.compile('.*totalTime=(.*);.*')

# open output file
oFile = open(HOME + "/" + OFNAME, 'w')
oFile.write("size\tmap_method\tmap_phase\treduce_method\treduce_phase\ttotal\n")    # TODO set

# parse files
for folder in IFOLDERS:
    print(HOME + "/" + folder + "/" + IFNAME)
    iFile = open(HOME + "/" + folder + "/" + IFNAME, 'r')
    # reset sums
    mm = 0
    mp = 0
    mCounter = 0
    rm = 0
    rp = 0
    rCounter = 0
    total = 0
    tCounter = 0
    for line in iFile:
        # match regex - TODO set
        match = reMapMethod.match(line)
        if match:
            mm = mm + float(match.group(1))
            mCounter += 1
        match = reMapPhase.match(line)
        if match:
            mp = mp + float(match.group(1))
        match = reReduceMethod.match(line)
        if match:
            rm = rm + float(match.group(1))
            rCounter += 1
        match = reReducePhase.match(line)
        if match:
            rp = rp + float(match.group(1))
        match = reTotalTime.match(line)
        if match:
            total = total + float(match.group(1))
            tCounter += 1
        
    iFile.close()
    
    #print("mm: " + str(mm/mCounter))
    #print("mp: " + str(mp/mCounter))
    #print("rm: " + str(rm/rCounter))
    #print("rp: " + str(rp/rCounter))
    #print("total: " + str(total/tCounter))
    
    # write to output - TODO set
    oFile.write(getSize(folder) + '\t' + str(mm/mCounter) + '\t' + str(mp/mCounter) + '\t')
    oFile.write(str(rm/rCounter) + '\t' + str(rp/rCounter) + '\t'+ str(total/tCounter) + '\n')
    
oFile.close()
print("Save to: " + HOME + "/" + OFNAME)
print("Done!")
    