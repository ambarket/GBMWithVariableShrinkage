hostsFile=$1
script=$2
if [ $# -eq 0 ]; then
    echo "No arguments provided"
    exit 1
fi

echo -e `./killRunningResearchOnAllHosts.sh $hostsFile`

`javac -cp ../src/plotStuff.jar ../src/Main.java ../src/parameterTuning/*.java ../src/parameterTuning/plotting/*.java ../src/regressionTree/*.java ../src/gbm/*.java ../src/gbm/cv/*.java ../src/dataset/*.java ../src/utilities/*.java`

for host in $(cat $hostsFile); do
    load=`ssh "$host" "cd git/GBMWithVariableShrinkage/scripts; ./cpuLoad.sh";`
    if [ ${load%.*} -lt 10 ];
    then
        ssh "$host" "cd git/GBMWithVariableShrinkage; nohup ./${script} > ./consoleOutput/${host}.out 2>&1&";
        echo "started ${script} on ${host} which only had ${load}% CPU usage"
    else
        echo "won't start ${script} on ${host} which already has ${load}% CPU usage"
    fi
done
