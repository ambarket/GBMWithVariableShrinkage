dsName=$1
if [ $# -eq 0 ]; then
    echo "No arguments provided"
    exit 1
fi

find "../locks/4/${dsName}" -mindepth 6 -maxdepth 6 -type d '!' -exec sh -c 'ls -1 "{}"|egrep -i -q "*doneLock.txt"' ';' -print | grep -v RevisedVariable
find "../locks/4/${dsName}" -mindepth 7 -maxdepth 7 -type d '!' -exec sh -c 'ls -1 "{}"|egrep -i -q "*doneLock.txt"' ';' -print | grep -v Constant
