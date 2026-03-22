package com.akazukin.web.config;

import com.akazukin.adapter.core.SnsAdapterFactory;
import com.akazukin.adapter.core.SnsAnalyticsAdapterFactory;
import com.akazukin.adapter.core.SnsGraphAdapterFactory;
import com.akazukin.application.usecase.SnsAdapterLookup;
import com.akazukin.application.usecase.SnsAnalyticsAdapterLookup;
import com.akazukin.application.usecase.SnsGraphAdapterLookup;
import com.akazukin.domain.port.SnsAnalyticsAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.Collection;

@ApplicationScoped
public class AppConfig {

    @Produces
    @Singleton
    public SnsAdapterFactory snsAdapterFactory() {
        return SnsAdapterFactory.create();
    }

    @Produces
    @Singleton
    public SnsAdapterLookup snsAdapterLookup(SnsAdapterFactory factory) {
        return factory::getAdapter;
    }

    @Produces
    @Singleton
    public SnsGraphAdapterFactory snsGraphAdapterFactory(SnsAdapterFactory snsAdapterFactory) {
        return SnsGraphAdapterFactory.fromSnsAdapterFactory(snsAdapterFactory);
    }

    @Produces
    @Singleton
    public SnsGraphAdapterLookup snsGraphAdapterLookup(SnsGraphAdapterFactory graphFactory) {
        return new SnsGraphAdapterLookup() {
            @Override
            public com.akazukin.domain.port.SnsGraphAdapter getAdapter(com.akazukin.domain.model.SnsPlatform platform) {
                return graphFactory.getAdapter(platform);
            }

            @Override
            public boolean supports(com.akazukin.domain.model.SnsPlatform platform) {
                return graphFactory.isSupported(platform);
            }
        };
    }

    @Produces
    @Singleton
    public SnsAnalyticsAdapterFactory snsAnalyticsAdapterFactory(SnsAdapterFactory snsAdapterFactory) {
        return SnsAnalyticsAdapterFactory.fromSnsAdapterFactory(snsAdapterFactory);
    }

    @Produces
    @Singleton
    public SnsAnalyticsAdapterLookup snsAnalyticsAdapterLookup(SnsAnalyticsAdapterFactory analyticsFactory) {
        return new SnsAnalyticsAdapterLookup() {
            @Override
            public SnsAnalyticsAdapter getAdapter(com.akazukin.domain.model.SnsPlatform platform) {
                return analyticsFactory.getAdapter(platform);
            }

            @Override
            public Collection<SnsAnalyticsAdapter> getAllAdapters() {
                return analyticsFactory.getAllAdapters();
            }

            @Override
            public boolean supports(com.akazukin.domain.model.SnsPlatform platform) {
                return analyticsFactory.isSupported(platform);
            }
        };
    }
}
