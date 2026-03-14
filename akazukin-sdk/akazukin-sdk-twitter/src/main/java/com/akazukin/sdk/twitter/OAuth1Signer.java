package com.akazukin.sdk.twitter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * OAuth 1.0a request signer for Twitter API v2.
 *
 * <p>Generates the {@code Authorization: OAuth ...} header required for
 * user-context operations (posting, deleting, etc.).</p>
 */
public class OAuth1Signer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String consumerKey;
    private final String consumerSecret;
    private final String accessToken;
    private final String accessTokenSecret;

    OAuth1Signer(String consumerKey, String consumerSecret,
                 String accessToken, String accessTokenSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
    }

    /**
     * Builds the OAuth 1.0a Authorization header value for the given HTTP method and URL.
     *
     * <p>Query parameters in the URL are extracted and included in the signature
     * base string as required by the OAuth 1.0a spec.</p>
     *
     * @param method HTTP method (GET, POST, DELETE, etc.)
     * @param url    full request URL (may include query string)
     * @return the Authorization header value, e.g. {@code OAuth oauth_consumer_key="...", ...}
     */
    String buildAuthorizationHeader(String method, String url) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = generateNonce();

        // Separate base URL from query parameters
        String baseUrl = url;
        TreeMap<String, String> params = new TreeMap<>();
        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            baseUrl = url.substring(0, queryIndex);
            for (String param : url.substring(queryIndex + 1).split("&")) {
                String[] kv = param.split("=", 2);
                params.put(percentDecode(kv[0]), kv.length > 1 ? percentDecode(kv[1]) : "");
            }
        }

        // OAuth parameters
        params.put("oauth_consumer_key", consumerKey);
        params.put("oauth_nonce", nonce);
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", timestamp);
        params.put("oauth_token", accessToken);
        params.put("oauth_version", "1.0");

        // Build signature base string
        String paramString = params.entrySet().stream()
            .map(e -> percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()))
            .collect(Collectors.joining("&"));

        String signatureBaseString = method.toUpperCase()
            + "&" + percentEncode(baseUrl)
            + "&" + percentEncode(paramString);

        // Compute HMAC-SHA1 signature
        String signingKey = percentEncode(consumerSecret) + "&" + percentEncode(accessTokenSecret);
        String signature = hmacSha1(signatureBaseString, signingKey);

        // Build Authorization header (only oauth_* params, not query params)
        TreeMap<String, String> oauthOnly = new TreeMap<>();
        oauthOnly.put("oauth_consumer_key", consumerKey);
        oauthOnly.put("oauth_nonce", nonce);
        oauthOnly.put("oauth_signature", signature);
        oauthOnly.put("oauth_signature_method", "HMAC-SHA1");
        oauthOnly.put("oauth_timestamp", timestamp);
        oauthOnly.put("oauth_token", accessToken);
        oauthOnly.put("oauth_version", "1.0");

        return "OAuth " + oauthOnly.entrySet().stream()
            .map(e -> percentEncode(e.getKey()) + "=\"" + percentEncode(e.getValue()) + "\"")
            .collect(Collectors.joining(", "));
    }

    private static String generateNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        // Use hex encoding for safe nonce (no special chars)
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String hmacSha1(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA1", e);
        }
    }

    /**
     * RFC 3986 percent-encoding (Twitter OAuth requires this, not URLEncoder's application/x-www-form-urlencoded).
     */
    static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
    }

    private static String percentDecode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
