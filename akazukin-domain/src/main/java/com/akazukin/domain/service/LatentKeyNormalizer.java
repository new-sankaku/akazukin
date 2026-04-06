package com.akazukin.domain.service;

public class LatentKeyNormalizer {

    protected String removeCharAt9and10(String latentKeyNumber) {
        if (latentKeyNumber.length() >= 10) {
            String target = latentKeyNumber.substring(8, 10);
            if ("E1".equals(target)) {
                latentKeyNumber = latentKeyNumber.substring(0, 8) + latentKeyNumber.substring(10);
            }
        }
        return latentKeyNumber;
    }
}
