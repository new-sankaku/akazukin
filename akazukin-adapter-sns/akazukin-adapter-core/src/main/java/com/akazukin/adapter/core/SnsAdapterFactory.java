package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public final class SnsAdapterFactory {

    private final Map<SnsPlatform, SnsAdapter> adapters;

    private SnsAdapterFactory(Map<SnsPlatform, SnsAdapter> adapters) {
        this.adapters = adapters;
    }

    public static SnsAdapterFactory create() {
        ServiceLoader<SnsAdapter> loader = ServiceLoader.load(SnsAdapter.class);
        Map<SnsPlatform, SnsAdapter> map = new HashMap<>();
        for (SnsAdapter adapter : loader) {
            map.put(adapter.platform(), adapter);
        }
        return new SnsAdapterFactory(Collections.unmodifiableMap(map));
    }

    public SnsAdapter getAdapter(SnsPlatform platform) {
        SnsAdapter adapter = adapters.get(platform);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for platform: " + platform);
        }
        return adapter;
    }

    public Set<SnsPlatform> getAvailablePlatforms() {
        return adapters.keySet();
    }

    public boolean isSupported(SnsPlatform platform) {
        return adapters.containsKey(platform);
    }
}
