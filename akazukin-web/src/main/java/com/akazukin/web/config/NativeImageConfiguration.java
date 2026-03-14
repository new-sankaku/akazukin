package com.akazukin.web.config;

import com.akazukin.application.dto.AccountResponseDto;
import com.akazukin.application.dto.AnalyticsResponseDto;
import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.dto.LoginRequestDto;
import com.akazukin.application.dto.LoginResponseDto;
import com.akazukin.application.dto.PostRequestDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.dto.PostTargetDto;
import com.akazukin.application.dto.RegisterRequestDto;
import com.akazukin.application.dto.TeamRequestDto;
import com.akazukin.application.dto.TeamResponseDto;
import com.akazukin.domain.model.NotificationType;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.SnsPlatform;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        ErrorResponseDto.class,
        LoginRequestDto.class,
        LoginResponseDto.class,
        RegisterRequestDto.class,
        PostRequestDto.class,
        PostResponseDto.class,
        PostTargetDto.class,
        AccountResponseDto.class,
        AnalyticsResponseDto.class,
        TeamRequestDto.class,
        TeamResponseDto.class,
        SnsPlatform.class,
        PostStatus.class,
        Role.class,
        NotificationType.class
})
public class NativeImageConfiguration {
}
