package com.akazukin.sdk.twitter.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2PkceFlowTest {

    private static final Pattern UNRESERVED_CHARS = Pattern.compile("^[A-Za-z0-9._~\\-=]+$");

    @Test
    void generateCodeVerifier_returnsStringOfCorrectLength() {
        String verifier = OAuth2PkceFlow.generateCodeVerifier();

        assertNotNull(verifier);
        assertTrue(verifier.length() >= 43,
                "Code verifier must be at least 43 characters, was: " + verifier.length());
        assertTrue(verifier.length() <= 128,
                "Code verifier must be at most 128 characters, was: " + verifier.length());
    }

    @Test
    void generateCodeVerifier_containsOnlyUnreservedChars() {
        String verifier = OAuth2PkceFlow.generateCodeVerifier();

        assertTrue(UNRESERVED_CHARS.matcher(verifier).matches(),
                "Code verifier must contain only unreserved characters: " + verifier);
    }

    @Test
    void generateCodeVerifier_returnsDifferentValuesEachCall() {
        String verifier1 = OAuth2PkceFlow.generateCodeVerifier();
        String verifier2 = OAuth2PkceFlow.generateCodeVerifier();

        assertNotEquals(verifier1, verifier2,
                "Each call should produce a unique verifier");
    }

    @Test
    void generateCodeChallenge_returnsBase64UrlEncodedSha256Hash() throws NoSuchAlgorithmException {
        String verifier = "test-code-verifier-value";

        String challenge = OAuth2PkceFlow.generateCodeChallenge(verifier);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String expectedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedHash);

        assertEquals(expectedChallenge, challenge);
    }

    @Test
    void generateCodeChallenge_returnsConsistentResultForSameInput() {
        String verifier = OAuth2PkceFlow.generateCodeVerifier();

        String challenge1 = OAuth2PkceFlow.generateCodeChallenge(verifier);
        String challenge2 = OAuth2PkceFlow.generateCodeChallenge(verifier);

        assertEquals(challenge1, challenge2);
    }

    @Test
    void generateCodeChallenge_returnsDifferentResultForDifferentInput() {
        String challenge1 = OAuth2PkceFlow.generateCodeChallenge("verifier-one");
        String challenge2 = OAuth2PkceFlow.generateCodeChallenge("verifier-two");

        assertNotEquals(challenge1, challenge2);
    }

    @Test
    void generateCodeChallenge_doesNotContainPadding() {
        String verifier = OAuth2PkceFlow.generateCodeVerifier();

        String challenge = OAuth2PkceFlow.generateCodeChallenge(verifier);

        assertTrue(!challenge.contains("="),
                "Code challenge should not contain Base64 padding characters");
    }
}
