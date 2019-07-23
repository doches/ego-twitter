package net.texasexpat.ego;

import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.client.Client;
import org.apache.commons.io.IOUtils;
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

    public static final String TOPIC = "brexit-twitter";

    public static void main(String[] args) {
        Configuration configuration = null;
        try {
            configuration = Configuration.read(args[0]);
        } catch (IOException x) {
            logger.error("Could not read configuration", x);
        }

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
        properties.put("group.id", "brexit-twitter");

        try (KafkaProducer<Integer, String> kafkaProducer = new KafkaProducer<>(properties)) {
            while (true) {
                List<String> tweets = twitter.fetch(ImmutableList.of("brexit"));
                tweets.forEach(tweet -> {
                    if (!seenTweets.contains(tweet)) {
                        logger.debug(tweet);
                        kafkaProducer.send(new ProducerRecord<>(TOPIC, tweet));
                        seenTweets.add(tweet);
                    }
                });
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
