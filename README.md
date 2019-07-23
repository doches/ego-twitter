# Twitter Pipeline

`ego-twitter` is just a place for me to experiment with building high-throughput pipelines for natural language processing. 

# Getting Started

#### 1. Kafka

First, you'll need to start up a local Kafka + Zookeeper cluster for local dev

    ./scripts/start.sh

You can stop this cluster at any time using:

    ./scripts/stop.sh

#### 2. Twitter

In order to obtain data to process, you'll probably want to run the twitter producer. 
To do that, first fill in your Twitter API credentials in `pipeline/twitter-producer/src/dist/config.yml` (start with the template in `pipeline/twitter-producer/src/dist/config.yml.template`)

The producer runs forever until manually killed. You can run it from Intellij for local dev, or from the cli with `./gradlew twitter-producer:run`. While running, it will periodically poll twitter for search results based on the configured keywords and write the resulting raw tweets into Kafka for processing.

#### 3. Entity & Sentiment Extraction

As a proof-of-concept, this pipeline implements a consumer that reads tweets from Kafka and runs them through a multi-stage NLP pipeline built with the Stanford CoreNLP libraries. This consumer extracts named entities and annotates each with an estimated sentiment rating (-1 for most negative, +1 for most positive) based on the sentiment of the surrounding sentence.

Note that this processing assumes the text is in English, for now.

You can run the consumer via Intellij for local dev, or from the CLI with `./gradlew twitter-consumer:run`. It will poll Kafka for incoming tweets, batch-process them, and write out a running count of the mean sentiment for each found named entity into (by default) a `results.txt` file.