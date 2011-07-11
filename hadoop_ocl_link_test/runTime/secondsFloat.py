#!/usr/bin/env python
# Python 3
import time
import sys

if sys.argv[1] == 'get':
    print(time.time())
if sys.argv[1] == 'diff':
    print(str(float(sys.argv[2]) - float(sys.argv[3])))