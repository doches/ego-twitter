package net.texasexpat.ego;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.internal.Strings;
import net.texasexpat.ego.config.Configuration;

public class FileProducer {
    private static Logger logger = LoggerFactory.getLogger(FileProducer.class.getCanonicalName());

    public static void main(String[] args) throws IOException {
        Configuration configuration = null;
        try {
            configuration = Configuration.read(args[0]);
        } catch (IOException x) {
            logger.error("Could not read configuration", x);
            System.exit(1);
        }

        final String topic = configuration.getKafka().getTopic();
        logger.debug("Writing to topic " + topic);

        Properties properties = new Properties();
        properties.put("bootstrap.servers", Strings.join(configuration.getKafka().getBootstrapServers(), ","));
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try (KafkaProducer<Integer, String> kafkaProducer = new KafkaProducer<>(properties);
             BufferedReader fin = new BufferedReader(new FileReader(configuration.getInputFile()))) {
            String line = fin.readLine();
            while (line != null) {
                logger.debug(line);
                kafkaProducer.send(new ProducerRecord<>(topic, line));
                line = fin.readLine();
            }
        }
    }
}
