package com.prioritize.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.ProjectRequest;
import com.prioritize.dto.ProjectResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.ProjectMapper;
import com.prioritize.model.Project;
import com.prioritize.model.ProjectStatus;
import com.prioritize.repository.CategoryRepository;
import com.prioritize.repository.GoalRepository;
import com.prioritize.repository.ProjectRepository;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final GoalRepository goalRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            CategoryRepository categoryRepository,
            GoalRepository goalRepository,
            ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.goalRepository = goalRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID userId, ProjectStatus status, UUID goalId) {
        return projectRepository.findFiltered(userId, status, goalId).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID userId, UUID projectId) {
        return projectMapper.toResponse(requireOwned(userId, projectId));
    }

    public ProjectResponse create(UUID userId, ProjectRequest request) {
        validateForeignKeys(userId, request.categoryId(), request.goalId());
        Project project = new Project();
        project.setUserId(userId);
        projectMapper.applyCreate(project, request);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(UUID userId, UUID projectId, ProjectRequest request) {
        Project project = requireOwned(userId, projectId);
        validateForeignKeys(userId, request.categoryId(), request.goalId());
        projectMapper.applyUpdate(project, request);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    public void delete(UUID userId, UUID projectId) {
        Project project = requireOwned(userId, projectId);
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public Project requireOwned(UUID userId, UUID projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private void validateForeignKeys(UUID userId, UUID categoryId, UUID goalId) {
        if (categoryId != null) {
            categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }
        if (goalId != null) {
            goalRepository.findByIdAndUserId(goalId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        }
    }
}
