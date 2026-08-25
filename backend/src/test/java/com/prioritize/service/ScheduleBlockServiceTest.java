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
import org.springframework.http.HttpStatus;

import com.prioritize.dto.ScheduleBlockRequest;
import com.prioritize.dto.ScheduleBlockResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.ScheduleBlockMapper;
import com.prioritize.model.ScheduleBlock;
import com.prioritize.model.Task;
import com.prioritize.repository.ScheduleBlockRepository;
import com.prioritize.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleBlockServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BLOCK_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TASK_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final Instant START = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant END = Instant.parse("2026-03-01T11:00:00Z");

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private TaskRepository taskRepository;

    private ScheduleBlockService scheduleBlockService;

    @BeforeEach
    void setUp() {
        scheduleBlockService = new ScheduleBlockService(
                scheduleBlockRepository, taskRepository, new ScheduleBlockMapper());
    }

    @Test
    void createPersistsBlockForOwnedTask() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.of(new Task()));
        when(scheduleBlockRepository.save(any(ScheduleBlock.class))).thenAnswer(invocation -> {
            ScheduleBlock block = invocation.getArgument(0);
            if (block.getId() == null) {
                block.setId(BLOCK_ID);
            }
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            if (block.getCreatedAt() == null) {
                block.setCreatedAt(now);
                block.setUpdatedAt(now);
            }
            return block;
        });

        ScheduleBlockResponse response = scheduleBlockService.create(
                USER_A, new ScheduleBlockRequest(TASK_ID, START, END, null));

        ArgumentCaptor<ScheduleBlock> captor = ArgumentCaptor.forClass(ScheduleBlock.class);
        verify(scheduleBlockRepository).save(captor.capture());
        ScheduleBlock saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.getTaskId()).isEqualTo(TASK_ID);
        assertThat(saved.isCompleted()).isFalse();
        assertThat(response.id()).isEqualTo(BLOCK_ID);
        assertThat(response.taskId()).isEqualTo(TASK_ID);
    }

    @Test
    void createRejectsEndBeforeOrEqualStart() {
        assertThatThrownBy(() -> scheduleBlockService.create(
                        USER_A, new ScheduleBlockRequest(TASK_ID, START, START, false)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessage("endAt must be after startAt");

        verify(scheduleBlockRepository, never()).save(any());
        verify(taskRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void createReturns404WhenTaskNotOwned() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleBlockService.create(
                        USER_A, new ScheduleBlockRequest(TASK_ID, START, END, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found");

        verify(scheduleBlockRepository, never()).save(any());
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(scheduleBlockRepository.findByIdAndUserId(BLOCK_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleBlockService.get(USER_B, BLOCK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Schedule block not found");
    }

    @Test
    void listRejectsPartialWindowParams() {
        assertThatThrownBy(() -> scheduleBlockService.list(USER_A, START, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessage("from and to must both be provided or both omitted");
    }
}
