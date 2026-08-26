package com.prioritize.sms;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.prioritize.config.TwilioProperties;

public class TwilioSmsSender implements SmsSender {

    private final TwilioProperties properties;
    private final RestClient restClient;

    public TwilioSmsSender(TwilioProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void send(String e164To, String body) {
        String accountSid = properties.getAccountSid();
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", e164To);
        form.add("From", properties.getFromNumber());
        form.add("Body", body);

        String basicAuth = Base64.getEncoder().encodeToString(
                (accountSid + ":" + properties.getAuthToken()).getBytes(StandardCharsets.UTF_8));

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(truncate("Twilio SMS failed: HTTP " + response.getStatusCode().value()));
            }
        } catch (RestClientResponseException ex) {
            throw new RuntimeException(truncate("Twilio SMS failed: " + extractMessage(ex)));
        }
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    private static String extractMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return body;
        }
        return ex.getStatusCode() + " " + ex.getStatusText();
    }

    private static String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Twilio SMS failed";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
