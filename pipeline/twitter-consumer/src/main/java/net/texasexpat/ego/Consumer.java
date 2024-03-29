package net.texasexpat.ego;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.io.IOUtils;
import joptsimple.internal.Strings;
import net.texasexpat.ego.config.Configuration;

public class Consumer {
    static Logger logger = LoggerFactory.getLogger(Consumer.class.getCanonicalName());

    public static void main(String[] args) {
        Configuration configuration = null;
        try {
            configuration = Configuration.read(args[0]);
        } catch (IOException x) {
            logger.error("Could not read configuration", x);
            System.exit(1);
        }

        Properties properties = new Properties();
        properties.put("bootstrap.servers", Strings.join(configuration.getKafka().getBootstrapServers(), ","));
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("group.id", configuration.getKafka().getGroupId());

        String outputFile = configuration.getOutputFile();

        EntitySentimentExtractor extractor = new EntitySentimentExtractor();

        Map<String, Pair<Integer, Double>> sentiments = Maps.newHashMap();

        if (Files.exists(Path.of(outputFile))) {
            logger.debug("Reading existing ratings from " + outputFile);
            IOUtils.readLines(outputFile).forEach(line -> {
                if (line.strip().length() > 0) {
                    String[] components = line.split("\t");
                    String name = components[0];
                    Double mean = Double.valueOf(components[1]);
                    Integer count = Integer.valueOf(components[2]);
                    sentiments.put(name, Pair.of(count, mean * count));
                }
            });
        }

        try (KafkaConsumer<Integer, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            List<String> topics = ImmutableList.of(configuration.getKafka().getTopic());
            kafkaConsumer.subscribe(topics);
            while (true) {
                ConsumerRecords<Integer, String> records = kafkaConsumer.poll(Duration.of(60, ChronoUnit.SECONDS));
                int index = 0;
                for (ConsumerRecord record : records) {
                    List<EntitySentimentRating> ratings = extractor.apply(record.value().toString()).stream()
                            .filter(rating -> rating.getEntityType().equalsIgnoreCase("person")
                                    && rating.getConfidence() > 0.5).collect(Collectors.toList());
                    index++;
                    if (index % 25 == 0) {
                        writeRatings(outputFile, sentiments);
                    }
                    if (ratings.size() > 0) {
                        logger.debug(String.format("Extracted sentiment ratings for %d documents", ratings.size()));
                    } else {
                        continue;
                    }
                    for (EntitySentimentRating rating : ratings) {
                        if (!sentiments.containsKey(rating.getEntity())) {
                            sentiments.put(rating.getEntity(), Pair.of(0, 0.0));
                        }
                        Pair<Integer, Double> counts = sentiments.get(rating.getEntity());
                        sentiments.put(
                                rating.getEntity(),
                                Pair.of(counts.getLeft() + 1, counts.getRight() + rating.getSentiment())
                        );
                    }
                }

                writeRatings(outputFile, sentiments);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void writeRatings(String outputFile, Map<String, Pair<Integer, Double>> sentiments) throws IOException {
        logger.debug("Saving sentiment ratings to " + outputFile);
        Map<Integer, String> lines = Maps.newHashMap();
        for (Map.Entry<String, Pair<Integer, Double>> entry : sentiments.entrySet()) {
            double sentiment = entry.getValue().getRight() / entry.getValue().getLeft();
            lines.put(entry.getValue().getLeft(), String.format("%s\t%.2f\t%d", entry.getKey(), sentiment, entry.getValue().getLeft()));
        }

        List<String> sorted = lines.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        IOUtils.writeStringToFile(
                String.join("\n", Lists.reverse(sorted)),
                outputFile,
                "utf-8"
        );
    }
}
