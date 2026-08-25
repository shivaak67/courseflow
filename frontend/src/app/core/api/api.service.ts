import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssignmentDto,
  CalendarEventDto,
  CanvasSyncResult,
  CategoryDto,
  CourseDto,
  CreateCalendarEventRequest,
  CreateCategoryRequest,
  CreateGoalRequest,
  CreateProjectRequest,
  CreateScheduleBlockRequest,
  CreateStudySessionRequest,
  CreateTaskRequest,
  DashboardSummary,
  GoalDto,
  PrioritizedAssignment,
  ProjectDto,
  ScheduleBlockDto,
  StudySessionDto,
  TaskDto,
  UpdateCalendarEventRequest,
  UpdateCategoryRequest,
  UpdateGoalRequest,
  UpdateProjectRequest,
  UpdateScheduleBlockRequest,
  UpdateTaskRequest,
} from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  dashboardSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.base}/api/dashboard/summary`);
  }

  prioritizedAssignments(): Observable<PrioritizedAssignment[]> {
    return this.http.get<PrioritizedAssignment[]>(`${this.base}/api/assignments/prioritized`);
  }

  listAssignments(): Observable<AssignmentDto[]> {
    return this.http.get<AssignmentDto[]>(`${this.base}/api/assignments`);
  }

  listCourses(): Observable<CourseDto[]> {
    return this.http.get<CourseDto[]>(`${this.base}/api/courses`);
  }

  listStudySessions(): Observable<StudySessionDto[]> {
    return this.http.get<StudySessionDto[]>(`${this.base}/api/study-sessions`);
  }

  createStudySession(body: CreateStudySessionRequest): Observable<StudySessionDto> {
    return this.http.post<StudySessionDto>(`${this.base}/api/study-sessions`, body);
  }

  syncCanvas(): Observable<CanvasSyncResult> {
    return this.http.post<CanvasSyncResult>(`${this.base}/api/canvas/sync`, {});
  }

  // --- Categories ---

  listCategories(): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>(`${this.base}/api/categories`);
  }

  createCategory(body: CreateCategoryRequest): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${this.base}/api/categories`, body);
  }

  updateCategory(id: string, body: UpdateCategoryRequest): Observable<CategoryDto> {
    return this.http.put<CategoryDto>(`${this.base}/api/categories/${id}`, body);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/categories/${id}`);
  }

  // --- Goals ---

  listGoals(): Observable<GoalDto[]> {
    return this.http.get<GoalDto[]>(`${this.base}/api/goals`);
  }

  createGoal(body: CreateGoalRequest): Observable<GoalDto> {
    return this.http.post<GoalDto>(`${this.base}/api/goals`, body);
  }

  updateGoal(id: string, body: UpdateGoalRequest): Observable<GoalDto> {
    return this.http.put<GoalDto>(`${this.base}/api/goals/${id}`, body);
  }

  deleteGoal(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/goals/${id}`);
  }

  // --- Projects ---

  listProjects(): Observable<ProjectDto[]> {
    return this.http.get<ProjectDto[]>(`${this.base}/api/projects`);
  }

  createProject(body: CreateProjectRequest): Observable<ProjectDto> {
    return this.http.post<ProjectDto>(`${this.base}/api/projects`, body);
  }

  updateProject(id: string, body: UpdateProjectRequest): Observable<ProjectDto> {
    return this.http.put<ProjectDto>(`${this.base}/api/projects/${id}`, body);
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/projects/${id}`);
  }

  // --- Tasks ---

  listTasks(): Observable<TaskDto[]> {
    return this.http.get<TaskDto[]>(`${this.base}/api/tasks`);
  }

  createTask(body: CreateTaskRequest): Observable<TaskDto> {
    return this.http.post<TaskDto>(`${this.base}/api/tasks`, body);
  }

  updateTask(id: string, body: UpdateTaskRequest): Observable<TaskDto> {
    return this.http.put<TaskDto>(`${this.base}/api/tasks/${id}`, body);
  }

  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/tasks/${id}`);
  }

  // --- Schedule blocks ---

  listScheduleBlocks(from: string, to: string): Observable<ScheduleBlockDto[]> {
    return this.http.get<ScheduleBlockDto[]>(`${this.base}/api/schedule-blocks`, {
      params: { from, to },
    });
  }

  createScheduleBlock(body: CreateScheduleBlockRequest): Observable<ScheduleBlockDto> {
    return this.http.post<ScheduleBlockDto>(`${this.base}/api/schedule-blocks`, body);
  }

  updateScheduleBlock(id: string, body: UpdateScheduleBlockRequest): Observable<ScheduleBlockDto> {
    return this.http.put<ScheduleBlockDto>(`${this.base}/api/schedule-blocks/${id}`, body);
  }

  deleteScheduleBlock(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/schedule-blocks/${id}`);
  }

  // --- Calendar events ---

  listCalendarEvents(from: string, to: string): Observable<CalendarEventDto[]> {
    return this.http.get<CalendarEventDto[]>(`${this.base}/api/calendar-events`, {
      params: { from, to },
    });
  }

  createCalendarEvent(body: CreateCalendarEventRequest): Observable<CalendarEventDto> {
    return this.http.post<CalendarEventDto>(`${this.base}/api/calendar-events`, body);
  }

  updateCalendarEvent(id: string, body: UpdateCalendarEventRequest): Observable<CalendarEventDto> {
    return this.http.put<CalendarEventDto>(`${this.base}/api/calendar-events/${id}`, body);
  }

  deleteCalendarEvent(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/calendar-events/${id}`);
  }
}
