package com.akazukin.domain.port;

public interface MediaStorage {

    String upload(String fileName, String mimeType, byte[] data);

    void delete(String storageUrl);

    byte[] download(String storageUrl);
}
