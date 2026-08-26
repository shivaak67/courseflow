package com.prioritize.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev/mock SMS sender: logs the message and always succeeds.
 */
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String e164To, String body) {
        log.info("SMS mock send to {}: {}", e164To, body);
    }

    @Override
    public boolean isConfigured() {
        return true;
    }
}
