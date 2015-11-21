hostsFile="allHosts.txt"
script=$1
maxHosts=$2
if [ $# -lt 2 -o $# -gt 3 ] 
then
    echo "Can only take 2 or 3 arguments"
    echo "Usage ./startResearchOnSpecifiedNumberOfHosts.sh scriptToRun.sh numberOfHostsToUse [killRunningResearchFlag]"
    exit 1
fi
killRunningResearch=$3
if [ -z "$killRunningResearch" ];
then
    echo "No killRunningResearchFlag specififed, defaulting to true"
    killRunningResearch=1
fi 
if [ $killRunningResearch -gt 0 ];
then
    echo "Killing all running Research"
    echo -e `./killRunningResearchOnAllHosts.sh $hostsFile`
else
    echo "Flag specified not to kill already running research"
fi

echo "Recompiling project"
`javac -cp "../jars/*" ../src/Main.java ../src/parameterTuning/*.java ../src/parameterTuning/plotting/*.java ../src/regressionTree/*.java ../src/gbm/*.java ../src/gbm/cv/*.java ../src/dataset/*.java ../src/utilities/*.java`

count=0;
for host in $(cat $hostsFile); do
    load=`ssh "$host" "cd git/GBMWithVariableShrinkage/scripts; ./cpuLoad.sh";`
    if [ ${load%.*} -lt 10 ];
    then
        if [ $count -lt $maxHosts ];
        then
            ssh "$host" "cd git/GBMWithVariableShrinkage; nohup ./${script} > ./consoleOutput/${host}.out 2> ./errorOutput/${host}.out &";
            echo "started ${script} on ${host} which only had ${load}% CPU usage"
            count=$(($count + 1));
        else
            echo "Max count of ${maxHosts} has been reached"
            break;
        fi
    else
        echo "won't start ${script} on ${host} which already has ${load}% CPU usage"
    fi
done
