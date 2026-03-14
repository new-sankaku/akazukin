package com.akazukin.application.usecase;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsInteractionAdapter;

/**
 * Abstraction for looking up SNS interaction adapters by platform.
 * Returns null if the platform doesn't support interactions.
 * Implemented in the web module to bridge adapter implementations.
 */
public interface SnsInteractionAdapterLookup {

    SnsInteractionAdapter getAdapter(SnsPlatform platform);

    boolean supports(SnsPlatform platform);
}
