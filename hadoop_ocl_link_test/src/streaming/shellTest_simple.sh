#!/usr/bin/env sh
make clean
make

echo "foo bar" | ./mapSimple | ./reduceSimple
