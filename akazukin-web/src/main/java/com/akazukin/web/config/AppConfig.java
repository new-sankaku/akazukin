package com.akazukin.web.config;

import com.akazukin.adapter.core.SnsAdapterFactory;
import com.akazukin.application.usecase.SnsAdapterLookup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

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
}
