package com.prioritize.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.TaskRequest;
import com.prioritize.dto.TaskResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.TaskMapper;
import com.prioritize.model.Task;
import com.prioritize.model.TaskStatus;
import com.prioritize.repository.CategoryRepository;
import com.prioritize.repository.ProjectRepository;
import com.prioritize.repository.TaskRepository;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final TaskMapper taskMapper;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            CategoryRepository categoryRepository,
            TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID userId, UUID projectId, TaskStatus status, UUID categoryId) {
        return taskRepository.findFiltered(userId, projectId, status, categoryId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID userId, UUID taskId) {
        return taskMapper.toResponse(requireOwned(userId, taskId));
    }

    public TaskResponse create(UUID userId, TaskRequest request) {
        validateForeignKeys(userId, request.projectId(), request.categoryId());
        Task task = new Task();
        task.setUserId(userId);
        taskMapper.applyCreate(task, request);
        applyCompletedAt(task, null);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    public TaskResponse update(UUID userId, UUID taskId, TaskRequest request) {
        Task task = requireOwned(userId, taskId);
        validateForeignKeys(userId, request.projectId(), request.categoryId());
        TaskStatus previousStatus = task.getStatus();
        taskMapper.applyUpdate(task, request);
        applyCompletedAt(task, previousStatus);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    public void delete(UUID userId, UUID taskId) {
        Task task = requireOwned(userId, taskId);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public Task requireOwned(UUID userId, UUID taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private void validateForeignKeys(UUID userId, UUID projectId, UUID categoryId) {
        if (projectId != null) {
            projectRepository.findByIdAndUserId(projectId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        }
        if (categoryId != null) {
            categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }
    }

    private void applyCompletedAt(Task task, TaskStatus previousStatus) {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            if (previousStatus != TaskStatus.COMPLETED) {
                task.setCompletedAt(Instant.now());
            }
        } else {
            task.setCompletedAt(null);
        }
    }
}
