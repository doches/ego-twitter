package net.texasexpat.ego;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoProducer {
    static Logger logger = LoggerFactory.getLogger(DemoProducer.class.getCanonicalName());

    public static final String TOPIC = "novel";

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try (KafkaProducer<Integer, String> kafkaProducer = new KafkaProducer<>(properties)) {
            logger.debug("Reading input");
            List<String> lines = IOUtils.readLines(new BufferedReader(new InputStreamReader(DemoProducer.class.getClassLoader().getResourceAsStream("monte_cristo.txt"))));

            for (int i = 0; i < lines.size(); i++) {
                logger.debug("Producing line " + i);
                kafkaProducer.send(new ProducerRecord<>(TOPIC, lines.get(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
