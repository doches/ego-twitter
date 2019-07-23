package net.texasexpat.ego;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer {
    static Logger logger = LoggerFactory.getLogger(Consumer.class.getCanonicalName());

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("group.id", "novel-group");

        EntitySentimentExtractor extractor = new EntitySentimentExtractor();

        try (KafkaConsumer<Integer, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            List<String> topics = ImmutableList.of("novel");
            kafkaConsumer.subscribe(topics);
            while (true) {
                ConsumerRecords<Integer, String> records = kafkaConsumer.poll(Duration.of(5, ChronoUnit.SECONDS));
                for (ConsumerRecord record : records) {
                    List<EntitySentimentRating> ratings = extractor.apply(record.value().toString()).stream()
                            .filter(rating -> rating.getEntityType().equalsIgnoreCase("person")
                                    && rating.getConfidence() > 0.5).collect(Collectors.toList());
                    for (EntitySentimentRating rating : ratings) {
                        logger.debug(String.format("%s (%s): %.1f", rating.getEntity(), rating.getEntityType(), rating.getSentiment()));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
