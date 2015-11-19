if ps aux | grep -v "grep" | grep "amb6470" | grep "research_amb647" >> /dev/null
then
    echo "Research is still running on ${HOSTNAME}"
else
   echo "Research is stopped on ${HOSTNAME}"
fi
