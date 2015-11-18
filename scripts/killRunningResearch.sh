if ps aux | grep -v "grep" | grep "amb6470" | grep "research_amb647" >> /dev/null
then
    echo -e "Research is still running on ${HOSTNAME}\n"

    # Getting the PID of the process
    PID=`pgrep research_amb647`

    # Number of seconds to wait before using "kill -9"
    WAIT_SECONDS=10

    # Counter to keep count of how many seconds have passed
    count=0

    while kill $PID > /dev/null
    do
        # Wait for one second
        sleep 1
        # Increment the second counter
        ((count++))

        # Has the process been killed? If so, exit the loop.
        if ! ps -p $PID > /dev/null ; then
            break
        fi

        # Have we exceeded $WAIT_SECONDS? If so, kill the process with "kill -9"
        # and exit the loop
        if [ $count -gt $WAIT_SECONDS ]; then
            kill -9 $PID
            break
        fi
    done
    echo -e "Process has been killed after $count seconds.\n"    
else
   echo -e "Research is stopped on ${HOSTNAME}\n"
fi
