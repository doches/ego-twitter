package net.texasexpat.ego;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntitySentimentExtractorTest {
    static EntitySentimentExtractor extractor;

    @BeforeClass
    public static void setup() {
        extractor = new EntitySentimentExtractor();
    }

    @Test
    public void testSimpleEntity() {
        List<EntitySentimentRating> ratings = extractor.apply("The city of Los Angeles");
        assertEquals("Returns exactly one entity rating", 1, ratings.size());
    }

    @Test
    public void testNegativeSentiment() {
        List<EntitySentimentRating> ratings = extractor.apply("The god-damn, horrible city of Los Angeles is a flaming hell-hole");
        assertEquals("Returns exactly one entity", 1, ratings.size());
        assertTrue("Returns negative sentiment", ratings.get(0).getSentiment() < 0);
    }

    @Test
    public void testPositiveSentiment() {
        List<EntitySentimentRating> ratings = extractor.apply("Edinburgh is a lovely city full where the people are friendly and the whiskey is delicious");
        for (EntitySentimentRating rating : ratings) {
            assertTrue("Returns positive sentiment", rating.getSentiment() > 0);
        }
    }
}