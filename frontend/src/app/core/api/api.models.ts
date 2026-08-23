export interface WorkloadByCourse {
  courseId: string;
  courseName: string;
  assignmentCount: number;
  estimatedHours: number;
}

export interface DashboardSummary {
  dueTodayCount: number;
  dueThisWeekCount: number;
  overdueCount: number;
  highPriorityCount: number;
  completedCount: number;
  remainingCount: number;
  estimatedHoursRemainingThisWeek: number;
  workloadByCourse: WorkloadByCourse[];
}

export type PriorityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface PrioritizedAssignment {
  id: string;
  courseId: string;
  courseName: string;
  title: string;
  dueDate: string | null;
  estimatedHours: number | null;
  priorityScore: number | null;
  priorityLevel: PriorityLevel | null;
  reasons: string[];
}

export interface CanvasSyncResult {
  coursesUpserted: number;
  assignmentsUpserted: number;
  lastSyncedAt: string;
}

export interface CourseDto {
  id: string;
  canvasCourseId: string | null;
  name: string;
  courseCode: string | null;
  term: string | null;
}
