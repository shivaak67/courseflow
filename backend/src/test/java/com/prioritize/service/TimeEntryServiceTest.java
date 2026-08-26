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

import com.prioritize.dto.TimeEntryRequest;
import com.prioritize.dto.TimeEntryResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.TimeEntryMapper;
import com.prioritize.model.Task;
import com.prioritize.model.TimeEntry;
import com.prioritize.repository.TaskRepository;
import com.prioritize.repository.TimeEntryRepository;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTRY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID TASK_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private TaskRepository taskRepository;

    private TimeEntryService timeEntryService;

    @BeforeEach
    void setUp() {
        timeEntryService = new TimeEntryService(
                timeEntryRepository, taskRepository, new TimeEntryMapper());
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(timeEntryRepository.findByIdAndUserId(ENTRY_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.get(USER_B, ENTRY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Time entry not found");
    }

    @Test
    void createUpdatesTaskActualMinutesFromSum() {
        Task task = ownedTask(USER_A, TASK_ID, 0);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.of(task));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> {
            TimeEntry entry = invocation.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(ENTRY_ID);
            }
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            if (entry.getCreatedAt() == null) {
                entry.setCreatedAt(now);
                entry.setUpdatedAt(now);
            }
            return entry;
        });
        when(timeEntryRepository.sumDurationByUserIdAndTaskId(USER_A, TASK_ID)).thenReturn(45);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeEntryResponse response = timeEntryService.create(
                USER_A, new TimeEntryRequest(TASK_ID, null, null, 45, "focused work"));

        ArgumentCaptor<TimeEntry> entryCaptor = ArgumentCaptor.forClass(TimeEntry.class);
        verify(timeEntryRepository).save(entryCaptor.capture());
        TimeEntry saved = entryCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.getTaskId()).isEqualTo(TASK_ID);
        assertThat(saved.getDurationMinutes()).isEqualTo(45);
        assertThat(response.durationMinutes()).isEqualTo(45);
        assertThat(task.getActualMinutes()).isEqualTo(45);
        verify(taskRepository).save(task);
    }

    @Test
    void createReturns404WhenTaskNotOwned() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.create(
                        USER_A, new TimeEntryRequest(TASK_ID, null, null, 30, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found");

        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void deleteRecalculatesActualMinutesIncludingZeroWhenLastEntry() {
        TimeEntry entry = ownedEntry(USER_A, ENTRY_ID, TASK_ID, 30);
        Task task = ownedTask(USER_A, TASK_ID, 30);
        when(timeEntryRepository.findByIdAndUserId(ENTRY_ID, USER_A)).thenReturn(Optional.of(entry));
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.of(task));
        when(timeEntryRepository.sumDurationByUserIdAndTaskId(USER_A, TASK_ID)).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        timeEntryService.delete(USER_A, ENTRY_ID);

        verify(timeEntryRepository).delete(entry);
        assertThat(task.getActualMinutes()).isZero();
        verify(taskRepository).save(task);
    }

    private Task ownedTask(UUID userId, UUID taskId, int actualMinutes) {
        Task task = new Task();
        task.setId(taskId);
        task.setUserId(userId);
        task.setTitle("Write report");
        task.setActualMinutes(actualMinutes);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    private TimeEntry ownedEntry(UUID userId, UUID entryId, UUID taskId, int durationMinutes) {
        TimeEntry entry = new TimeEntry();
        entry.setId(entryId);
        entry.setUserId(userId);
        entry.setTaskId(taskId);
        entry.setDurationMinutes(durationMinutes);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        return entry;
    }
}
