import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CanvasSyncResult,
  CourseDto,
  DashboardSummary,
  PrioritizedAssignment,
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

  listCourses(): Observable<CourseDto[]> {
    return this.http.get<CourseDto[]>(`${this.base}/api/courses`);
  }

  syncCanvas(): Observable<CanvasSyncResult> {
    return this.http.post<CanvasSyncResult>(`${this.base}/api/canvas/sync`, {});
  }
}
