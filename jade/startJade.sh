#!/bin/bash

HOST=localhost
PORT=1099
LOCALPORT=1098
CLASSPATH=`pwd`/lib/jade.jar
BOOT=jade.Boot
PLATFORM_ID=DomoticPlatform

java -cp $CLASSPATH $BOOT -host $HOST -port $PORT -platform-id $PLATFORM_ID
