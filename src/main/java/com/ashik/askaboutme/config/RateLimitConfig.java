package com.ashik.askaboutme.config;

import com.ashik.askaboutme.configdto.AskRateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AskRateLimitProperties.class)
public class RateLimitConfig {
}
