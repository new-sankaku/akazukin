package com.akazukin.web.security;

import com.akazukin.domain.model.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class JwtTokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "akazukin")
    String issuer;

    @ConfigProperty(name = "akazukin.jwt.access-token-duration", defaultValue = "PT15M")
    Duration accessTokenDuration;

    @ConfigProperty(name = "akazukin.jwt.refresh-token-duration", defaultValue = "P7D")
    Duration refreshTokenDuration;

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
}
