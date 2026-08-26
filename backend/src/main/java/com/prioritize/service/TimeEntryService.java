package com.prioritize.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.TimeEntryRequest;
import com.prioritize.dto.TimeEntryResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.TimeEntryMapper;
import com.prioritize.model.Task;
import com.prioritize.model.TimeEntry;
import com.prioritize.repository.TaskRepository;
import com.prioritize.repository.TimeEntryRepository;

@Service
@Transactional
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final TaskRepository taskRepository;
    private final TimeEntryMapper timeEntryMapper;

    public TimeEntryService(
            TimeEntryRepository timeEntryRepository,
            TaskRepository taskRepository,
            TimeEntryMapper timeEntryMapper) {
        this.timeEntryRepository = timeEntryRepository;
        this.taskRepository = taskRepository;
        this.timeEntryMapper = timeEntryMapper;
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> list(UUID userId, UUID taskId) {
        List<TimeEntry> entries = taskId != null
                ? timeEntryRepository.findByUserIdAndTaskIdOrderByCreatedAtDesc(userId, taskId)
                : timeEntryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return entries.stream().map(timeEntryMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TimeEntryResponse get(UUID userId, UUID entryId) {
        return timeEntryMapper.toResponse(requireOwned(userId, entryId));
    }

    public TimeEntryResponse create(UUID userId, TimeEntryRequest request) {
        requireOwnedTask(userId, request.taskId());
        int durationMinutes = resolveDurationMinutes(request);
        TimeEntry entry = new TimeEntry();
        entry.setUserId(userId);
        timeEntryMapper.applyCreate(entry, request, durationMinutes);
        TimeEntry saved = timeEntryRepository.save(entry);
        refreshTaskActualMinutes(userId, request.taskId());
        return timeEntryMapper.toResponse(saved);
    }

    public TimeEntryResponse update(UUID userId, UUID entryId, TimeEntryRequest request) {
        TimeEntry entry = requireOwned(userId, entryId);
        UUID previousTaskId = entry.getTaskId();
        requireOwnedTask(userId, request.taskId());
        int durationMinutes = resolveDurationMinutes(request);
        timeEntryMapper.applyUpdate(entry, request, durationMinutes);
        TimeEntry saved = timeEntryRepository.save(entry);
        refreshTaskActualMinutes(userId, request.taskId());
        if (!previousTaskId.equals(request.taskId())) {
            refreshTaskActualMinutes(userId, previousTaskId);
        }
        return timeEntryMapper.toResponse(saved);
    }

    public void delete(UUID userId, UUID entryId) {
        TimeEntry entry = requireOwned(userId, entryId);
        UUID taskId = entry.getTaskId();
        timeEntryRepository.delete(entry);
        refreshTaskActualMinutes(userId, taskId);
    }

    @Transactional(readOnly = true)
    public TimeEntry requireOwned(UUID userId, UUID entryId) {
        return timeEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Time entry not found"));
    }

    private void requireOwnedTask(UUID userId, UUID taskId) {
        taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private void refreshTaskActualMinutes(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        int sum = timeEntryRepository.sumDurationByUserIdAndTaskId(userId, taskId);
        task.setActualMinutes(sum);
        taskRepository.save(task);
    }

    private int resolveDurationMinutes(TimeEntryRequest request) {
        Instant startedAt = request.startedAt();
        Instant endedAt = request.endedAt();
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endedAt must be greater than or equal to startedAt");
        }
        if (request.durationMinutes() != null) {
            return request.durationMinutes();
        }
        if (startedAt != null && endedAt != null) {
            return (int) ChronoUnit.MINUTES.between(startedAt, endedAt);
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "durationMinutes is required when startedAt and endedAt are not both provided");
    }
}
