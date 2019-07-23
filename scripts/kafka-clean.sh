#!/bin/bash

KAFKA_DIR=../kafka

echo "topic:"
read topicName
$KAFKA_DIR/bin/kafka-configs.sh --zookeeper localhost:2181 --alter --entity-type topics --entity-name $topicName --add config retention.ms=1000
sleep 5
$KAFKA_DIR/bin/kafka-configs.sh --zookeeper localhost:2181 --alter --entity-type topics --entity-name $topicName --delete-config retention.ms
