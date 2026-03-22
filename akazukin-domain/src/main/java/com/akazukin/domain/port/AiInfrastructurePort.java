package com.akazukin.domain.port;

import java.util.List;

public interface AiInfrastructurePort {

    boolean isOllamaAvailable();

    String getOllamaEndpoint();

    String getOllamaDefaultModel();

    List<String> listOllamaModels();
}
