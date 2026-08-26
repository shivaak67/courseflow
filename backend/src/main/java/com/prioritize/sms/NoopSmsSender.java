package com.prioritize.sms;

/**
 * Placeholder when Twilio is neither mocked nor fully configured.
 */
public class NoopSmsSender implements SmsSender {

    @Override
    public void send(String e164To, String body) {
        throw new IllegalStateException("SMS not configured");
    }

    @Override
    public boolean isConfigured() {
        return false;
    }
}
