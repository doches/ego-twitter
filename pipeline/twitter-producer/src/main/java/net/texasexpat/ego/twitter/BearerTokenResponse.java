package net.texasexpat.ego.twitter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableBearerTokenResponse.class)
public abstract class BearerTokenResponse {
    @JsonProperty("token_type")
    public abstract String getTokenType();
    @JsonProperty("access_token")
    public abstract String getAccessToken();
}
