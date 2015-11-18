hostsFile="allHosts.txt"
command="cd git/GBMWithVariableShrinkage/scripts; ./cpuLoad.sh;"

for host in $(cat ${hostsFile}); do 
    load=`ssh "$host" "$command";`
    echo "${host} has ${load}% CPU usage"
done
