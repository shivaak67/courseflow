/** Planning-core entities (Category / Goal / Project / Task) */

export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'ARCHIVED';
export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'ARCHIVED';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface CategoryDto {
  id: string;
  name: string;
  icon: string | null;
  color: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  icon?: string | null;
  color?: string | null;
}

export interface UpdateCategoryRequest {
  name?: string;
  icon?: string | null;
  color?: string | null;
}

export interface GoalDto {
  id: string;
  categoryId: string | null;
  title: string;
  description: string | null;
  targetDate: string | null;
  status: GoalStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGoalRequest {
  title: string;
  categoryId?: string | null;
  description?: string | null;
  targetDate?: string | null;
  status?: GoalStatus;
}

export interface UpdateGoalRequest {
  title?: string;
  categoryId?: string | null;
  description?: string | null;
  targetDate?: string | null;
  status?: GoalStatus;
}

export interface ProjectDto {
  id: string;
  categoryId: string | null;
  goalId: string | null;
  title: string;
  description: string | null;
  startDate: string | null;
  targetDate: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  title: string;
  categoryId?: string | null;
  goalId?: string | null;
  description?: string | null;
  startDate?: string | null;
  targetDate?: string | null;
  status?: ProjectStatus;
}

export interface UpdateProjectRequest {
  title?: string;
  categoryId?: string | null;
  goalId?: string | null;
  description?: string | null;
  startDate?: string | null;
  targetDate?: string | null;
  status?: ProjectStatus;
}

export interface TaskDto {
  id: string;
  categoryId: string | null;
  projectId: string | null;
  title: string;
  description: string | null;
  dueDate: string | null;
  dueTime: string | null;
  estimatedMinutes: number | null;
  actualMinutes: number;
  priority: TaskPriority;
  status: TaskStatus;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  categoryId?: string | null;
  projectId?: string | null;
  description?: string | null;
  dueDate?: string | null;
  dueTime?: string | null;
  estimatedMinutes?: number | null;
  priority?: TaskPriority;
  status?: TaskStatus;
}

export interface UpdateTaskRequest {
  title?: string;
  categoryId?: string | null;
  projectId?: string | null;
  description?: string | null;
  dueDate?: string | null;
  dueTime?: string | null;
  estimatedMinutes?: number | null;
  actualMinutes?: number;
  priority?: TaskPriority;
  status?: TaskStatus;
}

/** Manual schedule blocks (time-blocking) */

export interface ScheduleBlockDto {
  id: string;
  taskId: string;
  taskTitle?: string | null;
  startAt: string;
  endAt: string;
  completed: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateScheduleBlockRequest {
  taskId: string;
  startAt: string;
  endAt: string;
  completed?: boolean;
}

export interface UpdateScheduleBlockRequest {
  taskId: string;
  startAt: string;
  endAt: string;
  completed?: boolean;
}

/** Personal calendar events */

export interface CalendarEventDto {
  id: string;
  title: string;
  description: string | null;
  categoryId: string | null;
  startAt: string;
  endAt: string;
  allDay: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCalendarEventRequest {
  title: string;
  description?: string | null;
  categoryId?: string | null;
  startAt: string;
  endAt: string;
  allDay?: boolean;
}

export interface UpdateCalendarEventRequest {
  title?: string;
  description?: string | null;
  categoryId?: string | null;
  startAt?: string;
  endAt?: string;
  allDay?: boolean;
}

/** Routines */

export type RecurrenceType = 'DAILY' | 'WEEKLY' | 'SELECTED_WEEKDAYS' | 'MONTHLY';

export interface RoutineDto {
  id: string;
  categoryId: string | null;
  title: string;
  recurrenceType: RecurrenceType;
  daysOfWeek: string | null;
  intervalValue: number;
  startTime: string;
  endTime: string | null;
  startDate: string;
  endDate: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRoutineRequest {
  title: string;
  categoryId?: string | null;
  recurrenceType: RecurrenceType;
  daysOfWeek?: string | null;
  intervalValue?: number;
  startTime: string;
  endTime?: string | null;
  startDate: string;
  endDate?: string | null;
  active?: boolean;
}

export interface UpdateRoutineRequest {
  title: string;
  categoryId?: string | null;
  recurrenceType: RecurrenceType;
  daysOfWeek?: string | null;
  intervalValue?: number;
  startTime: string;
  endTime?: string | null;
  startDate: string;
  endDate?: string | null;
  active?: boolean;
}

export interface RoutineOccurrenceDto {
  routineId: string;
  title: string;
  date: string;
  startTime: string;
  endTime: string | null;
  recurrenceType: RecurrenceType;
}

/** User profile */

export interface UserProfileDto {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  timezone: string | null;
  phoneNumber: string | null;
  phoneVerified: boolean;
}

export interface UpdateUserProfileRequest {
  timezone?: string | null;
  phoneNumber?: string | null;
}

/** Reminders & notifications */

export type ReminderEntityType =
  | 'TASK'
  | 'SCHEDULE_BLOCK'
  | 'ROUTINE'
  | 'CALENDAR_EVENT'
  | 'GOAL';

export type NotificationChannel = 'IN_APP' | 'SMS' | 'EMAIL';

export type ReminderStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'SENT'
  | 'FAILED'
  | 'CANCELLED';

export interface ReminderDto {
  id: string;
  relatedEntityType: ReminderEntityType;
  relatedEntityId: string;
  reminderAt: string;
  channel: NotificationChannel;
  status: ReminderStatus;
  sentAt: string | null;
  attemptCount: number;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReminderRequest {
  relatedEntityType: ReminderEntityType;
  relatedEntityId: string;
  reminderAt: string;
  channel: NotificationChannel;
}

export interface UpdateReminderRequest {
  relatedEntityType?: ReminderEntityType;
  relatedEntityId?: string;
  reminderAt?: string;
  channel?: NotificationChannel;
  status?: ReminderStatus;
}

export interface NotificationDto {
  id: string;
  title: string;
  body: string | null;
  relatedEntityType: string | null;
  relatedEntityId: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationSettingsDto {
  smsEnabled: boolean;
  inAppEnabled: boolean;
  emailEnabled: boolean;
  defaultReminderOffsetsMinutes: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateNotificationSettingsRequest {
  smsEnabled: boolean;
  inAppEnabled: boolean;
  emailEnabled: boolean;
  defaultReminderOffsetsMinutes?: string | null;
}

/** Time entries & insights */

export interface TimeEntryDto {
  id: string;
  taskId: string;
  startedAt: string | null;
  endedAt: string | null;
  durationMinutes: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTimeEntryRequest {
  taskId: string;
  durationMinutes: number;
  notes?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
}

export interface UpdateTimeEntryRequest {
  taskId: string;
  durationMinutes: number;
  notes?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
}

export interface MinutesByDay {
  date: string;
  minutes: number;
}

export interface TopTaskByMinutes {
  taskId: string;
  title: string;
  minutes: number;
}

export interface InsightsSummary {
  from: string;
  to: string;
  tasksCreated: number;
  tasksCompleted: number;
  openTasks: number;
  totalMinutesLogged: number;
  estimatedMinutesOpen: number;
  completionRate: number;
  minutesByDay: MinutesByDay[];
  topTasksByMinutes: TopTaskByMinutes[];
}
