
echo `./killRunningResearchOnAllHosts.sh`
count=0;

: '
for host in $(cat hostsAll.txt); do
    echo "Count ${count}";
    if [ $count -lt 3 ];
    then
        load=`ssh "$host" "cd git/GBMWithVariableShrinkage; ./getCurrentCPULoad.sh";`
        if [ ${load%.*} -lt 10 ];
        then
            ssh "$host" "cd git/GBMWithVariableShrinkage; nohup ./compileNasa.sh > ./Nasa_${host}.out 2>&1&";
            echo "started nasa research on ${host} which only had ${load}% CPU usage"
            sleep 1;
            count=$(($count + 1));
        else
            echo "wont start nasa research on ${host} which alread has ${load}% CPU usage"
        fi
    else
        echo "Reached limit for nasa hosts"
        break;
    fi
done
'

for host in $(cat hostsAll.txt); do
    echo "Count ${count}";
    if [ $count -lt 4 ];
    then
        load=`ssh "$host" "cd git/GBMWithVariableShrinkage; ./getCurrentCPULoad.sh";`
        if [ ${load%.*} -lt 10 ];
        then
            ssh "$host" "cd git/GBMWithVariableShrinkage; nohup ./compilePowerPlant.sh > ./PowerPlant_${host}.out 2>&1&";
            echo "started power plant research on ${host} which only had ${load}% CPU usage"
            sleep 1;
            count=$(($count + 1));
        else
            echo "won't start power plant research on ${host} which alread has ${load}% CPU usage"
        fi
    else
        echo "Reached limit for powerPlant hosts"
        break;
    fi
done
for host in $(cat hostsAll.txt); do
    echo "Count ${count}";
    if [ $count -lt 8 ];
    then
        load=`ssh "$host" "cd git/GBMWithVariableShrinkage; ./getCurrentCPULoad.sh";`
        if [ ${load%.*} -lt 10 ];
        then
            ssh "$host" "cd git/GBMWithVariableShrinkage; nohup ./compileBikeSharing.sh > ./BikeSharing_${host}.out 2>&1&";
            echo "started bike sharing research on ${host} which only had ${load}% CPU usage"
            sleep 1;
            count=$(($count + 1));
        else
            echo "won't start bike sharing research on ${host} which alread has ${load}% CPU usage"
        fi
    else
        echo "Reached limit for bike sharing hosts"
        break;
    fi
done
