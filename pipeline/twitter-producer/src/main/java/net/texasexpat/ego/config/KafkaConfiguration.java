package net.texasexpat.ego.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableKafkaConfiguration.class)
@Value.Style(jdkOnly = true)
public abstract class KafkaConfiguration {
    public abstract List<String> getBootstrapServers();
}
