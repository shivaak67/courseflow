package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import com.prioritize.model.AppNotification;
import com.prioritize.model.NotificationChannel;
import com.prioritize.model.NotificationSettings;
import com.prioritize.model.Reminder;
import com.prioritize.model.ReminderEntityType;
import com.prioritize.model.ReminderStatus;
import com.prioritize.model.User;
import com.prioritize.repository.AppNotificationRepository;
import com.prioritize.repository.NotificationSettingsRepository;
import com.prioritize.repository.ReminderRepository;
import com.prioritize.repository.UserRepository;
import com.prioritize.service.ReminderContentResolver.ReminderContent;

@ExtendWith(MockitoExtension.class)
class ReminderDispatchServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REMINDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TASK_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-08-25T18:00:00Z");

    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private AppNotificationRepository appNotificationRepository;
    @Mock
    private NotificationSettingsRepository notificationSettingsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReminderContentResolver reminderContentResolver;
    @Mock
    private EmailNotificationService emailNotificationService;
    @Mock
    private SmsNotificationService smsNotificationService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ReminderDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        dispatchService = new ReminderDispatchService(
                reminderRepository,
                appNotificationRepository,
                notificationSettingsRepository,
                userRepository,
                reminderContentResolver,
                emailNotificationService,
                smsNotificationService,
                clock,
                transactionManager);
    }

    @Test
    void processReminderEmailSendsAndMarksSent() {
        Reminder reminder = processingReminder(NotificationChannel.EMAIL);
        User user = userWithEmail("student@example.com");
        NotificationSettings settings = enabledSettings(true, false);

        when(reminderRepository.claimPending(REMINDER_ID, NOW)).thenReturn(1);
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));
        when(notificationSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reminderContentResolver.resolve(USER_ID, ReminderEntityType.TASK, TASK_ID))
                .thenReturn(new ReminderContent("Exam prep", NOW, "Aug 25 · 6:00 PM"));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatchService.processReminder(REMINDER_ID);

        verify(emailNotificationService).send(eq("student@example.com"), eq("Reminder: Exam prep"), any());
        ArgumentCaptor<Reminder> reminderCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(reminderCaptor.capture());
        assertThat(reminderCaptor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
    }

    @Test
    void processReminderSmsWithoutVerifiedPhoneMarksFailed() {
        Reminder reminder = processingReminder(NotificationChannel.SMS);
        User user = userWithEmail("student@example.com");
        user.setPhoneNumber("+15551234567");
        user.setPhoneVerified(false);
        NotificationSettings settings = enabledSettings(false, true);

        when(reminderRepository.claimPending(REMINDER_ID, NOW)).thenReturn(1);
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));
        when(notificationSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reminderContentResolver.resolve(USER_ID, ReminderEntityType.TASK, TASK_ID))
                .thenReturn(new ReminderContent("Exam prep", NOW, "Aug 25 · 6:00 PM"));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatchService.processReminder(REMINDER_ID);

        verify(smsNotificationService, never()).send(any(), any());
        ArgumentCaptor<Reminder> reminderCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(reminderCaptor.capture());
        assertThat(reminderCaptor.getValue().getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(reminderCaptor.getValue().getFailureReason()).isEqualTo("phone not verified");
    }

    @Test
    void processReminderInAppCreatesNotificationAndMarksSent() {
        Reminder reminder = processingReminder(NotificationChannel.IN_APP);
        User user = userWithEmail("student@example.com");
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId(USER_ID);
        settings.setInAppEnabled(true);

        when(reminderRepository.claimPending(REMINDER_ID, NOW)).thenReturn(1);
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));
        when(notificationSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(settings));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reminderContentResolver.resolve(USER_ID, ReminderEntityType.TASK, TASK_ID))
                .thenReturn(new ReminderContent("Exam prep", NOW, "Aug 25 · 6:00 PM"));
        when(appNotificationRepository.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification notification = invocation.getArgument(0);
            notification.setId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
            notification.setCreatedAt(NOW);
            return notification;
        });
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatchService.processReminder(REMINDER_ID);

        verify(appNotificationRepository).save(any(AppNotification.class));
        ArgumentCaptor<Reminder> reminderCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(reminderCaptor.capture());
        assertThat(reminderCaptor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
    }

    private Reminder processingReminder(NotificationChannel channel) {
        Reminder reminder = new Reminder();
        reminder.setId(REMINDER_ID);
        reminder.setUserId(USER_ID);
        reminder.setRelatedEntityType(ReminderEntityType.TASK);
        reminder.setRelatedEntityId(TASK_ID);
        reminder.setReminderAt(NOW.minusSeconds(60));
        reminder.setChannel(channel);
        reminder.setStatus(ReminderStatus.PROCESSING);
        reminder.setAttemptCount(1);
        reminder.setCreatedAt(NOW.minusSeconds(3600));
        reminder.setUpdatedAt(NOW);
        return reminder;
    }

    private static User userWithEmail(String email) {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private static NotificationSettings enabledSettings(boolean email, boolean sms) {
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId(USER_ID);
        settings.setEmailEnabled(email);
        settings.setSmsEnabled(sms);
        settings.setInAppEnabled(false);
        return settings;
    }
}
