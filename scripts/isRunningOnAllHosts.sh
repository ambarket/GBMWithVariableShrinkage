#!/bin/bash
hostsFile=$1
if [ $# -eq 0 ]; then
    echo "No arguments provided"
    exit 1
fi
command="cd git/GBMWithVariableShrinkage/scripts; ./isRunning.sh;"

for host in $(cat $1); do ssh "$host" "$command"; done
