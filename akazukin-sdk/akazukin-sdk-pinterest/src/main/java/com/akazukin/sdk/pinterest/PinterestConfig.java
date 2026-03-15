package com.akazukin.sdk.pinterest;

public record PinterestConfig(
    String appId,
    String appSecret,
    String redirectUri
) {
    public PinterestConfig {
        appId = appId != null ? appId : "";
        appSecret = appSecret != null ? appSecret : "";
        redirectUri = redirectUri != null ? redirectUri : "";
    }
}
