package com.prioritize.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.ScheduleBlockRequest;
import com.prioritize.dto.ScheduleBlockResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.ScheduleBlockMapper;
import com.prioritize.model.ScheduleBlock;
import com.prioritize.repository.ScheduleBlockRepository;
import com.prioritize.repository.TaskRepository;

@Service
@Transactional
public class ScheduleBlockService {

    private final ScheduleBlockRepository scheduleBlockRepository;
    private final TaskRepository taskRepository;
    private final ScheduleBlockMapper scheduleBlockMapper;

    public ScheduleBlockService(
            ScheduleBlockRepository scheduleBlockRepository,
            TaskRepository taskRepository,
            ScheduleBlockMapper scheduleBlockMapper) {
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.taskRepository = taskRepository;
        this.scheduleBlockMapper = scheduleBlockMapper;
    }

    @Transactional(readOnly = true)
    public List<ScheduleBlockResponse> list(UUID userId, Instant from, Instant to) {
        validateWindowParams(from, to);
        List<ScheduleBlock> blocks = (from != null && to != null)
                ? scheduleBlockRepository.findOverlapping(userId, from, to)
                : scheduleBlockRepository.findByUserIdOrderByStartAtAsc(userId);
        return blocks.stream().map(scheduleBlockMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleBlockResponse get(UUID userId, UUID blockId) {
        return scheduleBlockMapper.toResponse(requireOwned(userId, blockId));
    }

    public ScheduleBlockResponse create(UUID userId, ScheduleBlockRequest request) {
        validateRange(request.startAt(), request.endAt());
        requireOwnedTask(userId, request.taskId());
        ScheduleBlock block = new ScheduleBlock();
        block.setUserId(userId);
        scheduleBlockMapper.applyCreate(block, request);
        return scheduleBlockMapper.toResponse(scheduleBlockRepository.save(block));
    }

    public ScheduleBlockResponse update(UUID userId, UUID blockId, ScheduleBlockRequest request) {
        ScheduleBlock block = requireOwned(userId, blockId);
        validateRange(request.startAt(), request.endAt());
        requireOwnedTask(userId, request.taskId());
        scheduleBlockMapper.applyUpdate(block, request);
        return scheduleBlockMapper.toResponse(scheduleBlockRepository.save(block));
    }

    public void delete(UUID userId, UUID blockId) {
        ScheduleBlock block = requireOwned(userId, blockId);
        scheduleBlockRepository.delete(block);
    }

    @Transactional(readOnly = true)
    public ScheduleBlock requireOwned(UUID userId, UUID blockId) {
        return scheduleBlockRepository.findByIdAndUserId(blockId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule block not found"));
    }

    private void requireOwnedTask(UUID userId, UUID taskId) {
        taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private void validateRange(Instant startAt, Instant endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
        }
    }

    private void validateWindowParams(Instant from, Instant to) {
        if ((from == null) != (to == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from and to must both be provided or both omitted");
        }
        if (from != null && !to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "to must be after from");
        }
    }
}
