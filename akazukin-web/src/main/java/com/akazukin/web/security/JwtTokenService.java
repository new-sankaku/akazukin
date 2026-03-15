package com.akazukin.web.security;

import com.akazukin.domain.model.User;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtTokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "akazukin")
    String issuer;

    @ConfigProperty(name = "akazukin.jwt.access-token-duration", defaultValue = "PT15M")
    Duration accessTokenDuration;

    @ConfigProperty(name = "akazukin.jwt.refresh-token-duration", defaultValue = "P7D")
    Duration refreshTokenDuration;

    @ConfigProperty(name = "mp.jwt.verify.publickey.location", defaultValue = "/publicKey.pem")
    String publicKeyLocation;

    public String generateAccessToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getId().toString())
                .groups(Set.of(user.getRole().name()))
                .expiresAt(Instant.now().plus(accessTokenDuration))
                .innerSign()
                .encrypt();
    }

    public String generateRefreshToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .expiresAt(Instant.now().plus(refreshTokenDuration))
                .innerSign()
                .encrypt();
    }

    /**
     * Parses and validates a refresh token.
     *
     * @param token the raw JWT refresh token string
     * @return the user UUID encoded as the token subject
     * @throws InvalidRefreshTokenException if the token is invalid, expired, or not a refresh token
     */
    public UUID parseRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token must not be empty");
        }

        JwtClaims claims;
        try {
            PublicKey publicKey = KeyUtils.readPublicKey(publicKeyLocation);
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireSubject()
                    .setExpectedIssuer(issuer)
                    .setVerificationKey(publicKey)
                    .build();
            claims = jwtConsumer.processToClaims(token);
        } catch (InvalidJwtException e) {
            throw new InvalidRefreshTokenException("Failed to parse refresh token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new InvalidRefreshTokenException("Failed to verify refresh token: " + e.getMessage(), e);
        }

        String tokenType;
        try {
            tokenType = claims.getStringClaimValue("type");
        } catch (Exception e) {
            tokenType = null;
        }
        if (!"refresh".equals(tokenType)) {
            throw new InvalidRefreshTokenException("Token is not a refresh token");
        }

        String subject;
        try {
            subject = claims.getSubject();
        } catch (Exception e) {
            throw new InvalidRefreshTokenException("Failed to extract subject from refresh token", e);
        }
        if (subject == null || subject.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token has no subject");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new InvalidRefreshTokenException("Invalid subject format in refresh token", e);
        }
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) {
            super(message);
        }

        public InvalidRefreshTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
