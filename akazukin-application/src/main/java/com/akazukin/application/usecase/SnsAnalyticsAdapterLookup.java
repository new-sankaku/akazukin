package com.akazukin.application.usecase;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAnalyticsAdapter;

import java.util.Collection;

public interface SnsAnalyticsAdapterLookup {

    SnsAnalyticsAdapter getAdapter(SnsPlatform platform);

    Collection<SnsAnalyticsAdapter> getAllAdapters();

    boolean supports(SnsPlatform platform);
}
