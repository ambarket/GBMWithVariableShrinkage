hostsFile="allHosts.txt"
command="cd git/GBMWithVariableShrinkage/scripts; ./isRunning.sh;"

for host in $(cat ${hostsFile}); do ssh "$host" "$command"; done
