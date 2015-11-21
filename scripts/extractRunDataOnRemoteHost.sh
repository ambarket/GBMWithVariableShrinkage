if [ $# -ne 2 ] 
then
    echo "Can only take 2 arguments"
    echo "Usage ./extract[...].sh datasetMinimalName tarGzFIleName"
    exit 1
fi
ssh ambarket.info "cd /mnt/raidZ_6TB/Austin/GBMWithVariableShrinkage/parameterTuning/4/${1}; tar -xzf ${2}"
