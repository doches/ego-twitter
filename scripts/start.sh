#!/bin/bash

./kafka/bin/zookeeper-server-start.sh ./kafka/config/zookeeper.properties &> zookeeper.log &
sleep 1
./kafka/bin/kafka-server-start.sh ./kafka/config/server.properties &> kafka.log &