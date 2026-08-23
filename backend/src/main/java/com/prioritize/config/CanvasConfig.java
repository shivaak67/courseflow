package com.prioritize.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CanvasProperties.class)
public class CanvasConfig {
}
