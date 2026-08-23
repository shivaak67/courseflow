package com.prioritize.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PrioritizeScoringProperties.class)
public class PrioritizeConfig {
}
