package net.texasexpat.ego.twitter;

public class TwitterException extends Exception {
    TwitterException(String message, Exception x) {
        super(message, x);
    }
}
