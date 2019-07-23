package net.texasexpat.ego;

import org.immutables.value.Value;

@Value.Immutable
public abstract class EntitySentimentRating {
    public abstract String getEntity();
    public abstract String getEntityType();
    public abstract Double getConfidence();
    public abstract Double getSentiment();
}
