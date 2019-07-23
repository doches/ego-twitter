package net.texasexpat.ego;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import joptsimple.internal.Strings;
import net.texasexpat.ego.config.Configuration;

public class FileProducer {
    public static void main(String[] args) throws IOException {
        Configuration configuration = null;
        try {
            configuration = Configuration.read(args[0]);
        } catch (IOException x) {
            System.err.println(x.getMessage());
            System.exit(1);
        }

        final String topic = configuration.getKafka().getTopic();

        Properties properties = new Properties();
        properties.put("bootstrap.servers", Strings.join(configuration.getKafka().getBootstrapServers(), ","));
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("group.id", configuration.getKafka().getGroupId());

        try (KafkaProducer<Integer, String> kafkaProducer = new KafkaProducer<>(properties);
             BufferedReader fin = new BufferedReader(new InputStreamReader(new BufferedInputStream(System.in)))) {
            String line = fin.readLine();
            while (line != null) {
                kafkaProducer.send(new ProducerRecord<>(topic, line));
                line = fin.readLine();
            }
        }
    }
}
