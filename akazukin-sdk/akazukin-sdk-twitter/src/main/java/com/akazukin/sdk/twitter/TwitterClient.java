package com.akazukin.sdk.twitter;

import java.net.http.HttpClient;

public class TwitterClient implements AutoCloseable {

    private final TwitterConfig config;
    private final HttpClient httpClient;

    private TwitterClient(TwitterConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() {
        // TODO: Clean up resources
    }

    public static class Builder {
        private TwitterConfig config;

        private Builder() {
        }

        public Builder config(TwitterConfig config) {
            this.config = config;
            return this;
        }

        public TwitterClient build() {
            if (config == null) {
                throw new IllegalStateException("TwitterConfig must be provided");
            }
            return new TwitterClient(config);
        }
    }
}
