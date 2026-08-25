package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prioritize.dto.TaskRequest;
import com.prioritize.dto.TaskResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.TaskMapper;
import com.prioritize.model.Task;
import com.prioritize.model.TaskPriority;
import com.prioritize.model.TaskStatus;
import com.prioritize.repository.CategoryRepository;
import com.prioritize.repository.ProjectRepository;
import com.prioritize.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TASK_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PROJECT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository, projectRepository, categoryRepository, new TaskMapper());
    }

    @Test
    void createPersistsManualPriorityWithoutAutoCalculation() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(TASK_ID);
            }
            if (task.getCreatedAt() == null) {
                Instant now = Instant.parse("2026-01-01T00:00:00Z");
                task.setCreatedAt(now);
                task.setUpdatedAt(now);
            }
            return task;
        });

        TaskResponse response = taskService.create(
                USER_A,
                new TaskRequest(
                        "Write report",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        TaskPriority.URGENT,
                        null));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.URGENT);
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(saved.getActualMinutes()).isZero();
        assertThat(response.priority()).isEqualTo(TaskPriority.URGENT);
        assertThat(response.actualMinutes()).isZero();
    }

    @Test
    void createDefaultsPriorityToMedium() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.create(
                USER_A,
                new TaskRequest("Sketch outline", null, null, null, null, null, null, null, null));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.get(USER_B, TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found");
    }

    @Test
    void createReturns404WhenProjectNotOwned() {
        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(
                        USER_A,
                        new TaskRequest("Write report", null, null, PROJECT_ID, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateSetsCompletedAtWhenMovingToCompleted() {
        Task task = ownedTask(USER_A, TaskStatus.TODO, null);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.update(
                USER_A,
                TASK_ID,
                new TaskRequest(
                        "Write report",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        TaskPriority.MEDIUM,
                        TaskStatus.COMPLETED));

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
        assertThat(task.getActualMinutes()).isZero();
    }

    @Test
    void updateClearsCompletedAtWhenLeavingCompleted() {
        Instant completedAt = Instant.parse("2026-01-02T12:00:00Z");
        Task task = ownedTask(USER_A, TaskStatus.COMPLETED, completedAt);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_A)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.update(
                USER_A,
                TASK_ID,
                new TaskRequest(
                        "Write report",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        TaskPriority.HIGH,
                        TaskStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.completedAt()).isNull();
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void listReturnsOnlyCallerTasks() {
        Task task = ownedTask(USER_A, TaskStatus.TODO, null);
        when(taskRepository.findFiltered(USER_A, null, null, null)).thenReturn(List.of(task));

        List<TaskResponse> responses = taskService.list(USER_A, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(TASK_ID);
        verify(taskRepository, never()).findAll();
    }

    private Task ownedTask(UUID userId, TaskStatus status, Instant completedAt) {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setUserId(userId);
        task.setTitle("Write report");
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(status);
        task.setActualMinutes(0);
        task.setCompletedAt(completedAt);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }
}
