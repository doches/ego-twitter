package net.texasexpat.ego.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.texasexpat.ego.config.TwitterConfig;

public class TwitterClientImpl implements TwitterClient {
    private static Logger logger = LoggerFactory.getLogger(TwitterClientImpl.class.getCanonicalName());
    private static ObjectMapper mapper = new ObjectMapper();

    public static final String TWITTER_BASE_URL = "https://api.twitter.com";

    private WebTarget target;
    private String bearerToken;

    public TwitterClientImpl(TwitterConfig config) throws TwitterException {
        ClientConfig clientConfig = new ClientConfig();
        HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic(config.getConsumerKey(), config.getConsumerSecret());
        Client client = ClientBuilder.newClient(clientConfig);
        client.register(authFeature);
        target = client.target(TWITTER_BASE_URL);

        Future<Response> future = target.path("/oauth2/token").request()
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .buildPost(Entity.entity("grant_type=client_credentials", MediaType.APPLICATION_FORM_URLENCODED))
                .submit();
        try {
            Response response = future.get();
            String body = extractResponse(response);
            BearerTokenResponse token = mapper.readValue(body, BearerTokenResponse.class);
            this.bearerToken = token.getAccessToken();
            logger.debug(String.format("Obtained token %s/%s", token.getTokenType(), token.getAccessToken()));
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new TwitterException("Could not obtain bearer token", e);
        }
    }

    public List<String> fetch(List<String> keywords) throws TwitterException {
        List<String> tweets = Lists.newArrayList();
        try {
            Future<Response> future = target.path("/1.1/search/tweets.json")
                    .queryParam("tweet_mode", "extended")
                    .queryParam("q", URLEncoder.encode(String.join(",", keywords), "utf-8"))
                    .request()
                    .header("Authorization", String.format("Bearer %s", bearerToken))
                    .buildGet()
                    .submit();
            Response response = future.get();
            String body = extractResponse(response);

            JsonNode tree = mapper.readTree(body);
            for(Iterator<JsonNode> iter = tree.get("statuses").elements(); iter.hasNext(); ) {
                JsonNode statusNode = iter.next();
                if (statusNode.has("retweeted_status")) {
                    statusNode = statusNode.get("retweeted_status");
                }
                String fullText = statusNode.get("full_text").asText();
                tweets.add(fullText);
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new TwitterException("Could not fetch tweets", e);
        }

        return tweets;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    private String extractResponse(Response response) throws IOException {
        if (response.getEntity() != null) {
            InputStream in = (InputStream)response.getEntity();
            StringWriter out = new StringWriter();
            IOUtils.copy(in, out, "UTF-8");
            return out.toString();
        }

        return "";
    }
}
