package com.prioritize.sms;

public interface SmsSender {

    void send(String e164To, String body);

    boolean isConfigured();
}
