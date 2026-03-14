package com.akazukin.infrastructure.cache;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RedisCacheService {

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;

    @Inject
    public RedisCacheService(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.keyCommands = redisDataSource.key(String.class);
    }

    public void put(String key, String value, Duration ttl) {
        valueCommands.setex(key, ttl.toSeconds(), value);
    }

    public Optional<String> get(String key) {
        String value = valueCommands.get(key);
        return Optional.ofNullable(value);
    }

    public void invalidate(String key) {
        keyCommands.del(key);
    }

    public void invalidateByPattern(String pattern) {
        List<String> keys = keyCommands.keys(pattern);
        if (!keys.isEmpty()) {
            keyCommands.del(keys.toArray(new String[0]));
        }
    }

    public boolean exists(String key) {
        return keyCommands.exists(key);
    }
}
