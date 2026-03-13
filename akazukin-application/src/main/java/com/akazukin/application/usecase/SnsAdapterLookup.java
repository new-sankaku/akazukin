package com.akazukin.application.usecase;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAdapter;

/**
 * Abstraction for looking up SNS adapters by platform.
 * This avoids a direct dependency from the application layer to adapter-core.
 * Implemented in the web module to bridge SnsAdapterFactory.
 */
public interface SnsAdapterLookup {

    SnsAdapter getAdapter(SnsPlatform platform);
}
