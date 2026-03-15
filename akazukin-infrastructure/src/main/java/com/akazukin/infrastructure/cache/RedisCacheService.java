package com.akazukin.infrastructure.cache;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RedisCacheService {

    private static final int SCAN_BATCH_SIZE = 100;

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
        var cursor = keyCommands.scan(new KeyScanArgs().match(pattern).count(SCAN_BATCH_SIZE));
        List<String> batch = new ArrayList<>();
        while (cursor.hasNext()) {
            batch.addAll(cursor.next());
            if (batch.size() >= SCAN_BATCH_SIZE) {
                keyCommands.del(batch.toArray(new String[0]));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            keyCommands.del(batch.toArray(new String[0]));
        }
    }

    public boolean exists(String key) {
        return keyCommands.exists(key);
    }
}
