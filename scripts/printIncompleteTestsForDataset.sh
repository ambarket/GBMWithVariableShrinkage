dsName=$1
runNum=$2
if [ $# -ne 2 ]; then
    echo "No arguments provided"
    exit 1
fi

find "../locks/4/${dsName}/${runNum}" -mindepth 5 -maxdepth 5 -type d '!' -exec sh -c 'ls -1 "{}"|egrep -i -q "*doneLock.txt"' ';' -print | grep -v RevisedVariable
find "../locks/4/${dsName}/${runNum}" -mindepth 6 -maxdepth 6 -type d '!' -exec sh -c 'ls -1 "{}"|egrep -i -q "*doneLock.txt"' ';' -print | grep -v Constant
