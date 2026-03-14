package com.akazukin.domain.port;

import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;

public interface AiTextGenerator {

    AiResponse generate(AiPrompt prompt);

    AiResponse generateWithPersona(AiPersona persona, String userInput);
}
