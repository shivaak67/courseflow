package com.prioritize.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.prioritize.config.NotificationProperties;
import com.prioritize.model.*;
import com.prioritize.repository.*;

class SmsReminderEligibilityTest {
    private final UUID id = UUID.randomUUID();
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationSettingsRepository settings = mock(NotificationSettingsRepository.class);
    private final NotificationProperties properties = new NotificationProperties();
    private final SmsReminderEligibility service = new SmsReminderEligibility(users, settings, properties);

    @Test void rejectsUnavailableSms() {
        assertThatThrownBy(() -> service.validate(id, NotificationChannel.SMS)).hasMessageContaining("temporarily unavailable");
    }
    @Test void requiresOptInAndVerifiedNumber() {
        properties.getSms().setEnabled(true);
        properties.getSms().setAccountSid("test");
        properties.getSms().setAuthToken("test");
        properties.getSms().setFromNumber("+15551234567");
        assertThatThrownBy(() -> service.validate(id, NotificationChannel.SMS)).hasMessageContaining("Enable SMS");
        NotificationSettings prefs = new NotificationSettings();
        prefs.setSmsEnabled(true);
        when(settings.findById(id)).thenReturn(Optional.of(prefs));
        User user = new User();
        user.setPhoneNumber("+15557654321");
        when(users.findById(id)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.validate(id, NotificationChannel.SMS)).hasMessageContaining("Verify your mobile");
        user.setPhoneVerified(true);
        assertThatCode(() -> service.validate(id, NotificationChannel.SMS)).doesNotThrowAnyException();
        prefs.setSmsEnabled(false);
        assertThatThrownBy(() -> service.validate(id, NotificationChannel.SMS)).hasMessageContaining("Enable SMS");
    }
}
