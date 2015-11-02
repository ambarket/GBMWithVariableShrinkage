javac -cp src/plotStuff.jar src/*.java src/regressionTree/*.java src/gbm/*.java src/gbm/cv/*.java src/dataset/*.java src/utilities/*.java

java -cp src/plotStuff.jar:src/:src/regressionTree:src/gbm:src/gbm/cv:src/dataset:src/utilities Main

