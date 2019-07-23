package net.texasexpat.ego;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.stream.Collectors;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.glassfish.jersey.internal.guava.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.internal.Strings;
import net.texasexpat.ego.config.Configuration;
import net.texasexpat.ego.twitter.TwitterClient;
import net.texasexpat.ego.twitter.TwitterClientImpl;
import net.texasexpat.ego.twitter.TwitterException;

public class TwitterProducer {
    static Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getCanonicalName());

    public static void main(String[] args) {
        Configuration configuration = null;
        try {
            configuration = Configuration.read(args[0]);
        } catch (IOException x) {
            logger.error("Could not read configuration", x);
        }

        final String topic = configuration.getKafka().getTopic();

        TwitterClient twitter = null;
        try {
            twitter = new TwitterClientImpl(configuration.getTwitter());
        } catch (TwitterException e) {
            logger.error("Could not instantiate twitter client", e);
        }

        HashSet<String> seenTweets = Sets.newHashSet();

        Properties properties = new Properties();
        properties.put("bootstrap.servers", Strings.join(configuration.getKafka().getBootstrapServers(), ","));
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try (KafkaProducer<Integer, String> kafkaProducer = new KafkaProducer<>(properties)) {
            Queue<String> keywordRotation = new ArrayDeque<>(configuration.getKeywords());
            while (true) {
                String keyword = keywordRotation.poll();
                keywordRotation.add(keyword);
                List<String> tweets = twitter.fetch(ImmutableList.of(keyword))
                        .stream()
                        .filter(tweet -> !seenTweets.contains(tweet))
                        .map(tweet -> tweet.replaceAll("\n", " "))
                        .distinct()
                        .collect(Collectors.toList());
                if (tweets.size() > 0) {
                    logger.info(String.format("Fetched %d new tweets", tweets.size()));
                }
                tweets.forEach(tweet -> {
                    logger.debug(tweet);
                    kafkaProducer.send(new ProducerRecord<>(topic, tweet));
                    seenTweets.add(tweet);
                });

                if (configuration.getSplitToFile() != null && tweets.size() > 0) {
                    try (FileWriter fout = new FileWriter(configuration.getSplitToFile(), true)) {
                        for (String tweet : tweets) {
                            fout.write(tweet);
                            fout.write('\n');
                        }
                        fout.flush();
                    }
                }

                Thread.sleep(configuration.getDelayMs());

                if (seenTweets.size() > configuration.getCacheDuplicateTweetCount()) {
                    seenTweets.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
