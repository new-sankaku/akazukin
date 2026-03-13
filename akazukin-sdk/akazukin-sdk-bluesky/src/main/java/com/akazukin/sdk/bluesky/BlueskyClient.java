package com.akazukin.sdk.bluesky;

import java.net.http.HttpClient;

public class BlueskyClient implements AutoCloseable {

    private final BlueskyConfig config;
    private final HttpClient httpClient;

    private BlueskyClient(BlueskyConfig config) {
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
        private BlueskyConfig config;

        private Builder() {
        }

        public Builder config(BlueskyConfig config) {
            this.config = config;
            return this;
        }

        public BlueskyClient build() {
            if (config == null) {
                throw new IllegalStateException("BlueskyConfig must be provided");
            }
            return new BlueskyClient(config);
        }
    }
}
