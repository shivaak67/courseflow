package com.prioritize.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.prioritize.sms.LoggingSmsSender;
import com.prioritize.sms.NoopSmsSender;
import com.prioritize.sms.SmsSender;
import com.prioritize.sms.TwilioSmsSender;

@Configuration
@EnableConfigurationProperties(TwilioProperties.class)
public class TwilioConfig {

    @Bean
    SmsSender smsSender(TwilioProperties properties, ObjectProvider<RestClient.Builder> restClientBuilder) {
        if (properties.isMock()) {
            return new LoggingSmsSender();
        }
        if (properties.isEnabled() && properties.hasCredentials()) {
            RestClient.Builder builder = restClientBuilder.getIfAvailable(RestClient::builder);
            return new TwilioSmsSender(properties, builder);
        }
        return new NoopSmsSender();
    }
}
