package com.akazukin.application.usecase;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsGraphAdapter;

/**
 * Abstraction for looking up SNS graph adapters by platform.
 * Implemented in the web module to bridge adapter implementations.
 */
public interface SnsGraphAdapterLookup {

    SnsGraphAdapter getAdapter(SnsPlatform platform);

    boolean supports(SnsPlatform platform);
}
