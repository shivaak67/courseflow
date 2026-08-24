import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssignmentDto,
  CanvasSyncResult,
  CourseDto,
  CreateStudySessionRequest,
  DashboardSummary,
  PrioritizedAssignment,
  StudySessionDto,
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
}
