package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAnalyticsAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SnsAnalyticsAdapterFactory {

    private final Map<SnsPlatform, SnsAnalyticsAdapter> adapters;

    private SnsAnalyticsAdapterFactory(Map<SnsPlatform, SnsAnalyticsAdapter> adapters) {
        this.adapters = adapters;
    }

    public static SnsAnalyticsAdapterFactory fromSnsAdapterFactory(SnsAdapterFactory snsAdapterFactory) {
        Map<SnsPlatform, SnsAnalyticsAdapter> map = new HashMap<>();
        for (SnsPlatform platform : snsAdapterFactory.getAvailablePlatforms()) {
            var adapter = snsAdapterFactory.getAdapter(platform);
            if (adapter instanceof SnsAnalyticsAdapter analyticsAdapter) {
                map.put(platform, analyticsAdapter);
            }
        }
        return new SnsAnalyticsAdapterFactory(Collections.unmodifiableMap(map));
    }

    public SnsAnalyticsAdapter getAdapter(SnsPlatform platform) {
        SnsAnalyticsAdapter adapter = adapters.get(platform);
        if (adapter == null) {
            throw new IllegalArgumentException("No analytics adapter registered for platform: " + platform);
        }
        return adapter;
    }

    public Collection<SnsAnalyticsAdapter> getAllAdapters() {
        return adapters.values();
    }

    public Set<SnsPlatform> getAvailablePlatforms() {
        return adapters.keySet();
    }

    public boolean isSupported(SnsPlatform platform) {
        return adapters.containsKey(platform);
    }
}
