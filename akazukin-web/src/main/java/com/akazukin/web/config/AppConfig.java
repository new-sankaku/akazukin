package com.akazukin.web.config;

import com.akazukin.adapter.core.SnsAdapterFactory;
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
}
