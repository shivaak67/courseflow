package com.prioritize.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.twilio")
public class TwilioProperties {

    private boolean enabled;
    private boolean mock = true;
    private String accountSid = "";
    private String authToken = "";
    private String fromNumber = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public boolean hasCredentials() {
        return isPresent(accountSid) && isPresent(authToken) && isPresent(fromNumber);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
