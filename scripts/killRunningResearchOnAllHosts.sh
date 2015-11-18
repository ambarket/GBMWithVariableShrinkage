#!/bin/bash

hostsFile="allHosts.txt"

command="cd git/GBMWithVariableShrinkage/scripts; ./killRunningResearch.sh;"

for host in $(cat $hostsFile); do 
    ssh "$host" "$command"; 
done

# Clean up any old locks hosts had on tests before I killed them
`find ../locks/ -type f -name '*--hostLock.txt' -delete`
