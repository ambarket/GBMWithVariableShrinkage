#!/bin/bash

hostsFile="allHosts.txt"

command="cd git/GBMWithVariableShrinkage/scripts; ./killRunningResearch.sh;"

for host in $(cat $hostsFile); do 
    ssh "$host" "$command"; 
done

count=0;
for host in $(cat $hostsFile); do
        if [ $count -lt $maxHosts ];
        then
            ssh "$host" "$command"; 
            `rm ../consoleOutput/${host}.out`
            `rm ../errorOutput/${host}.out`
            echo "killed research on ${host}" 
            count=$(($count + 1));
        else
            echo "Max count of ${maxHosts} has been reached"
            break;
        fi
done

# Clean up any old locks hosts had on tests before I killed them
`find ../locks/ -type f -name '*--hostLock.txt' -delete`
