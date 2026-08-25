package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.ScheduleBlockRequest;
import com.prioritize.dto.ScheduleBlockResponse;
import com.prioritize.model.ScheduleBlock;

@Component
public class ScheduleBlockMapper {

    public ScheduleBlockResponse toResponse(ScheduleBlock block) {
        return new ScheduleBlockResponse(
                block.getId(),
                block.getTaskId(),
                block.getStartAt(),
                block.getEndAt(),
                block.isCompleted(),
                block.getCreatedAt(),
                block.getUpdatedAt());
    }

    public void applyCreate(ScheduleBlock block, ScheduleBlockRequest request) {
        block.setTaskId(request.taskId());
        block.setStartAt(request.startAt());
        block.setEndAt(request.endAt());
        block.setCompleted(request.completed() != null && request.completed());
    }

    public void applyUpdate(ScheduleBlock block, ScheduleBlockRequest request) {
        block.setTaskId(request.taskId());
        block.setStartAt(request.startAt());
        block.setEndAt(request.endAt());
        if (request.completed() != null) {
            block.setCompleted(request.completed());
        }
    }
}
