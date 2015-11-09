loads=($(mpstat -P ALL 1 1 | awk '/Average:/ && $2 ~ /[0-9]/ {print $3}')); 
totalLoad=$(perl -e "print ${loads[0]} + ${loads[1]} + ${loads[2]} + ${loads[3]}")
echo $totalLoad
