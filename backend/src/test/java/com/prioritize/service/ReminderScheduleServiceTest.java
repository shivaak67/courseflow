package com.prioritize.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.prioritize.dto.ReminderScheduleRequest;
import com.prioritize.mapper.ReminderMapper;
import com.prioritize.model.*;
import com.prioritize.repository.*;

class ReminderScheduleServiceTest {
    private final UUID userId = UUID.randomUUID(), taskId = UUID.randomUUID();
    private final ReminderRepository reminders = mock(ReminderRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final SmsReminderEligibility eligibility = mock(SmsReminderEligibility.class);
    private final ReminderContentResolver resolver = new ReminderContentResolver(tasks,
        mock(ScheduleBlockRepository.class), mock(RoutineRepository.class), mock(CalendarEventRepository.class),
        mock(GoalRepository.class), users);
    private final ReminderScheduleService service = new ReminderScheduleService(mock(ReminderService.class),
        resolver, reminders, new ReminderMapper(), eligibility, users,
        Clock.fixed(Instant.parse("2026-09-07T17:00:00Z"), ZoneOffset.UTC));

    private User setupTask() {
        User user = new User();
        when(users.findById(userId)).thenReturn(Optional.of(user));
        Task task = new Task();
        task.setTitle("Assignment");
        task.setDueDate(LocalDate.of(2026, 9, 7));
        task.setDueTime(LocalTime.of(16, 0));
        when(tasks.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));
        return user;
    }
    @Test void schedulesInLocalTimezoneAndSkipsOnlyPastOffsets() {
        User user = setupTask();
        when(reminders.save(any(Reminder.class))).thenAnswer(i -> i.getArgument(0));
        var response = service.schedule(userId, new ReminderScheduleRequest(ReminderEntityType.TASK, taskId,
            List.of(1440, 30), List.of(NotificationChannel.SMS), "America/Chicago"));
        assertThat(user.getTimezone()).isEqualTo("America/Chicago");
        assertThat(response.reminders()).hasSize(1);
        assertThat(response.reminders().get(0).reminderAt()).isEqualTo(Instant.parse("2026-09-07T20:30:00Z"));
        verify(eligibility).validate(userId, NotificationChannel.SMS);
    }
    @Test void expiredOffsetsDoNotCancelExistingReminders() {
        setupTask();
        assertThatThrownBy(() -> service.schedule(userId, new ReminderScheduleRequest(ReminderEntityType.TASK, taskId,
            List.of(1440), List.of(NotificationChannel.SMS), "America/Chicago"))).hasMessageContaining("already passed");
        verifyNoInteractions(reminders);
    }
    @Test void invalidTimezoneDoesNotMutateReminders() {
        assertThatThrownBy(() -> service.schedule(userId, new ReminderScheduleRequest(ReminderEntityType.TASK, taskId,
            List.of(30), List.of(NotificationChannel.SMS), "invalid-zone"))).hasMessage("Invalid timezone");
        verifyNoInteractions(reminders, users);
    }
}
