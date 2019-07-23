package net.texasexpat.ego;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;

public class EntitySentimentExtractor {
    protected StanfordCoreNLP coreNLP;

    public EntitySentimentExtractor() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,sentiment");
        this.coreNLP = new StanfordCoreNLP(props);
    }

    public List<EntitySentimentRating> apply(String text) {
        if (text == null || text.replaceAll("/s/g", "").length() < 5) {
            return ImmutableList.of();
        }

        CoreDocument document = new CoreDocument(text);

        List<EntitySentimentRating> ratings = Lists.newArrayList();

        coreNLP.annotate(document);

        // Average sentiment over the whole document.
        Annotation annotation = document.annotation();
        Double average = annotation.get(CoreAnnotations.SentencesAnnotation.class).stream().map(sentence -> {
            String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
            if (sentiment.equalsIgnoreCase("negative")) {
                return -1.0;
            } else if (sentiment.equalsIgnoreCase("positive")) {
                return 1.0;
            }
            return 0;
        }).collect(Collectors.averagingDouble(Number::doubleValue));

        for (CoreEntityMention mention : document.entityMentions()) {
            Map<String, Double> confidence = mention.entityTypeConfidences();
            if (confidence.containsKey(mention.entityType())) {
                ratings.add(ImmutableEntitySentimentRating.builder()
                        .entity(mention.text())
                        .entityType(mention.entityType())
                        .confidence(confidence.get(mention.entityType()))
                        .sentiment(average)
                        .build());
            }
        }

        return ratings;
    }
}
