package net.texasexpat.ego.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableTwitterConfig.class)
public abstract class TwitterConfig {
    public abstract String getToken();
    public abstract String getSecret();
    public abstract String getConsumerKey();
    public abstract String getConsumerSecret();
}
