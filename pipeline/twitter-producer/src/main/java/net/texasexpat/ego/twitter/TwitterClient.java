package net.texasexpat.ego.twitter;

import java.util.List;

public interface TwitterClient {
    List<String> fetch(List<String> keywords) throws TwitterException;
}
