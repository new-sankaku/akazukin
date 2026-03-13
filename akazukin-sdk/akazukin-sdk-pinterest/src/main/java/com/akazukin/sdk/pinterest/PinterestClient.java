package com.akazukin.sdk.pinterest;

import java.net.http.HttpClient;

public class PinterestClient implements AutoCloseable {

    private final PinterestConfig config;
    private final HttpClient httpClient;

    private PinterestClient(PinterestConfig config) {
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
        private PinterestConfig config;

        private Builder() {
        }

        public Builder config(PinterestConfig config) {
            this.config = config;
            return this;
        }

        public PinterestClient build() {
            if (config == null) {
                throw new IllegalStateException("PinterestConfig must be provided");
            }
            return new PinterestClient(config);
        }
    }
}
