#!/bin/bash

hostsFile=$1
if [ $# -eq 0 ]; then
    echo "No arguments provided"
    exit 1
fi

`find ../data/ -type f -name '*--hostLock.txt' -delete`
command="cd git/GBMWithVariableShrinkage/scripts; ./killRunningResearch.sh;"

for host in $(cat $hostsFile); do 
    ssh "$host" "$command"; 
done
