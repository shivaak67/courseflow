package com.prioritize.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.prioritize.config.NotificationProperties;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    public EmailNotificationService(
            JavaMailSender mailSender,
            NotificationProperties notificationProperties) {
        this.mailSender = mailSender;
        this.notificationProperties = notificationProperties;
    }

    public void send(String to, String subject, String body) {
        if (!notificationProperties.getEmail().isEnabled()) {
            throw new NotificationDeliveryException("Email delivery is not enabled");
        }
        if (to == null || to.isBlank()) {
            throw new NotificationDeliveryException("Recipient email is missing");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(notificationProperties.getEmail().getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send email to {}: {}", to, ex.getMessage());
            throw new NotificationDeliveryException("Failed to send email");
        }
    }
}
