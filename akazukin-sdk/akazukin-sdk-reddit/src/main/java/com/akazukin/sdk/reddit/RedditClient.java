package com.akazukin.sdk.reddit;

import java.net.http.HttpClient;

public class RedditClient implements AutoCloseable {

    private final RedditConfig config;
    private final HttpClient httpClient;

    private RedditClient(RedditConfig config) {
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
        private RedditConfig config;

        private Builder() {
        }

        public Builder config(RedditConfig config) {
            this.config = config;
            return this;
        }

        public RedditClient build() {
            if (config == null) {
                throw new IllegalStateException("RedditConfig must be provided");
            }
            return new RedditClient(config);
        }
    }
}
