package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prioritize.dto.ReminderRequest;
import com.prioritize.dto.ReminderResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.ReminderMapper;
import com.prioritize.model.NotificationChannel;
import com.prioritize.model.Reminder;
import com.prioritize.model.ReminderEntityType;
import com.prioritize.model.ReminderStatus;
import com.prioritize.model.Task;
import com.prioritize.repository.CalendarEventRepository;
import com.prioritize.repository.GoalRepository;
import com.prioritize.repository.ReminderRepository;
import com.prioritize.repository.RoutineRepository;
import com.prioritize.repository.ScheduleBlockRepository;
import com.prioritize.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REMINDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TASK_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;
    @Mock
    private RoutineRepository routineRepository;
    @Mock
    private CalendarEventRepository calendarEventRepository;
    @Mock
    private GoalRepository goalRepository;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(
                reminderRepository,
                taskRepository,
                scheduleBlockRepository,
                routineRepository,
                calendarEventRepository,
                goalRepository,
                new ReminderMapper());
    }

    @Test
    void createRejectsRelatedEntityOwnedByAnotherUser() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.create(
                USER_A,
                new ReminderRequest(
                        ReminderEntityType.TASK,
                        TASK_ID,
                        Instant.parse("2026-08-25T18:00:00Z"),
                        NotificationChannel.EMAIL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("TASK not found");
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void createPersistsPendingReminderWhenEntityOwned() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setUserId(USER_A);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.of(task));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder reminder = invocation.getArgument(0);
            if (reminder.getId() == null) {
                reminder.setId(REMINDER_ID);
            }
            Instant now = Instant.parse("2026-08-25T12:00:00Z");
            reminder.setCreatedAt(now);
            reminder.setUpdatedAt(now);
            return reminder;
        });

        ReminderResponse response = reminderService.create(
                USER_A,
                new ReminderRequest(
                        ReminderEntityType.TASK,
                        TASK_ID,
                        Instant.parse("2026-08-25T18:00:00Z"),
                        null));

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        Reminder saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(response.id()).isEqualTo(REMINDER_ID);
        assertThat(response.status()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.get(USER_B, REMINDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reminder not found");
    }

    @Test
    void cancelSetsCancelledAndIsIdempotent() {
        Reminder reminder = pendingReminder(USER_A);
        when(reminderRepository.findByIdAndUserId(REMINDER_ID, USER_A)).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReminderResponse first = reminderService.cancel(USER_A, REMINDER_ID);
        assertThat(first.status()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(reminder);

        ReminderResponse second = reminderService.cancel(USER_A, REMINDER_ID);
        assertThat(second.status()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(any(Reminder.class));
    }

    private Reminder pendingReminder(UUID userId) {
        Reminder reminder = new Reminder();
        reminder.setId(REMINDER_ID);
        reminder.setUserId(userId);
        reminder.setRelatedEntityType(ReminderEntityType.TASK);
        reminder.setRelatedEntityId(TASK_ID);
        reminder.setReminderAt(Instant.parse("2026-08-25T18:00:00Z"));
        reminder.setChannel(NotificationChannel.IN_APP);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setAttemptCount(0);
        Instant now = Instant.parse("2026-08-25T12:00:00Z");
        reminder.setCreatedAt(now);
        reminder.setUpdatedAt(now);
        return reminder;
    }
}
