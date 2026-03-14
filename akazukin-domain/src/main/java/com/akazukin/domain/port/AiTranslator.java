package com.akazukin.domain.port;

import com.akazukin.domain.model.TranslationRequest;
import com.akazukin.domain.model.TranslationResult;

public interface AiTranslator {

    TranslationResult translate(TranslationRequest request);
}
