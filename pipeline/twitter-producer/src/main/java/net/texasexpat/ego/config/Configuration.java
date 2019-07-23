package net.texasexpat.ego.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableConfiguration.class)
public abstract class Configuration {
    public abstract TwitterConfig getTwitter();
    public abstract KafkaConfiguration getKafka();
    public abstract Long getDelayMs();
    public abstract Integer getCacheDuplicateTweetCount();

    public static Configuration read(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(path), Configuration.class);
    }
}
