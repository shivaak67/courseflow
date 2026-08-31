package com.prioritize.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.prioritize.config.NotificationProperties;

@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final NotificationProperties notificationProperties;
    private final RestClient restClient;

    public SmsNotificationService(NotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
        this.restClient = RestClient.create();
    }

    public void send(String to, String body) {
        NotificationProperties.Sms sms = notificationProperties.getSms();
        if (!sms.isConfigured()) {
            throw new NotificationDeliveryException("SMS delivery is not configured");
        }
        if (to == null || to.isBlank()) {
            throw new NotificationDeliveryException("Recipient phone number is missing");
        }

        String url = "https://api.twilio.com/2010-04-01/Accounts/"
                + sms.getAccountSid()
                + "/Messages.json";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", sms.getFromNumber());
        form.add("Body", body);

        String credentials = sms.getAccountSid() + ":" + sms.getAuthToken();
        String authHeader = "Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        try {
            restClient.post()
                    .uri(url)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Failed to send SMS to {}: {}", to, ex.getMessage());
            throw new NotificationDeliveryException("Failed to send SMS");
        }
    }
}
