package com.akazukin.web.security;

import com.akazukin.domain.model.User;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

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

    @Inject
    JWTParser jwtParser;

    public String generateAccessToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getId().toString())
                .groups(Set.of(user.getRole().name()))
                .expiresAt(Instant.now().plus(accessTokenDuration))
                .sign();
    }

    public String generateRefreshToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.getUsername())
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .expiresAt(Instant.now().plus(refreshTokenDuration))
                .sign();
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

        JsonWebToken jwt;
        try {
            jwt = jwtParser.parse(token);
        } catch (ParseException e) {
            throw new InvalidRefreshTokenException("Failed to parse refresh token: " + e.getMessage(), e);
        }

        if (!issuer.equals(jwt.getIssuer())) {
            throw new InvalidRefreshTokenException("Invalid token issuer");
        }

        String tokenType = jwt.getClaim("type");
        if (!"refresh".equals(tokenType)) {
            throw new InvalidRefreshTokenException("Token is not a refresh token");
        }

        if (jwt.getExpirationTime() <= Instant.now().getEpochSecond()) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        String subject = jwt.getSubject();
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
