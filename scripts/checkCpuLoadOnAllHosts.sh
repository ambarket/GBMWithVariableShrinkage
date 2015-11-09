hostsFile=$1
if [ $# -eq 0 ]; then
    echo "No arguments provided"
    exit 1
fi
for host in $(cat $1); do
        load=`ssh "$host" "cd git/GBMWithVariableShrinkage/scripts; ./cpuLoad.sh";`
        echo "${host} has ${load}% CPU usage"
done
