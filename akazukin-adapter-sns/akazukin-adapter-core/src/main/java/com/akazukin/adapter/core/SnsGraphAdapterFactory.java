package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsGraphAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SnsGraphAdapterFactory {

    private final Map<SnsPlatform, SnsGraphAdapter> adapters;

    private SnsGraphAdapterFactory(Map<SnsPlatform, SnsGraphAdapter> adapters) {
        this.adapters = adapters;
    }

    public static SnsGraphAdapterFactory fromSnsAdapterFactory(SnsAdapterFactory snsAdapterFactory) {
        Map<SnsPlatform, SnsGraphAdapter> map = new HashMap<>();
        for (SnsPlatform platform : snsAdapterFactory.getAvailablePlatforms()) {
            var adapter = snsAdapterFactory.getAdapter(platform);
            if (adapter instanceof SnsGraphAdapter graphAdapter) {
                map.put(platform, graphAdapter);
            }
        }
        return new SnsGraphAdapterFactory(Collections.unmodifiableMap(map));
    }

    public SnsGraphAdapter getAdapter(SnsPlatform platform) {
        SnsGraphAdapter adapter = adapters.get(platform);
        if (adapter == null) {
            throw new IllegalArgumentException("No graph adapter registered for platform: " + platform);
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
