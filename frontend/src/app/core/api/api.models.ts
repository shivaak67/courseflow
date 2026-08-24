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

export interface AssignmentDto {
  id: string;
  courseId: string;
  courseName: string;
  canvasAssignmentId: string | null;
  title: string;
  description: string | null;
  dueDate: string | null;
  pointsPossible: number | null;
  completed: boolean;
  submitted: boolean;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | null;
  estimatedHours: number | null;
  actualHours: number;
  personalPriority: number | null;
  priorityScore: number | null;
  priorityLevel: PriorityLevel | null;
  createdAt: string;
  updatedAt: string;
}

export interface StudySessionDto {
  id: string;
  assignmentId: string;
  assignmentTitle: string;
  courseName: string;
  startedAt: string | null;
  endedAt: string | null;
  durationMinutes: number;
  notes: string | null;
  createdAt: string;
}

export interface CreateStudySessionRequest {
  assignmentId: string;
  startedAt?: string | null;
  endedAt?: string | null;
  durationMinutes: number;
  notes?: string | null;
}
