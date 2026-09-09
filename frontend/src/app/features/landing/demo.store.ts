import { Injectable, signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import {
  CalendarEventDto,
  CreateCalendarEventRequest,
  TaskDto,
  UpdateCalendarEventRequest,
  UpdateTaskRequest,
} from '../../core/api/api.models';
import { dateKey } from '../../shared/planning-time';

/** Component-scoped fictional data; deliberately has no HTTP or storage dependency. */
@Injectable()
export class DemoStore {
  readonly tasks = signal<TaskDto[]>([]);
  readonly events = signal<CalendarEventDto[]>([]);
  constructor() {
    this.reset();
  }
  reset(): void {
    this.tasks.set([]);
    this.events.set([]);
    [
      'Outline history essay',
      'Review biology notes',
      'Practice presentation',
    ].forEach((title) => this.addTask(title));
    const start = new Date();
    start.setHours(10, 0, 0, 0);
    this.createCalendarEvent({
      title: 'Essay writing session',
      startAt: start.toISOString(),
      endAt: new Date(start.getTime() + 45 * 60000).toISOString(),
    });
  }
  addTask(title: string): void {
    const now = new Date().toISOString();
    this.tasks.update((tasks) => [
      ...tasks,
      {
        id: crypto.randomUUID(),
        title,
        dueDate: dateKey(new Date()),
        dueTime: null,
        status: 'TODO',
        priority: 'MEDIUM',
        categoryId: null,
        projectId: null,
        description: null,
        estimatedMinutes: 30,
        actualMinutes: 0,
        completedAt: null,
        createdAt: now,
        updatedAt: now,
      },
    ]);
  }
  listTasks() {
    return of(this.tasks());
  }
  listCalendarEvents(from: string, to: string) {
    return of(this.events().filter((e) => e.startAt < to && e.endAt > from));
  }
  updateTask(id: string, body: UpdateTaskRequest) {
    const existing = this.tasks().find((t) => t.id === id);
    if (!existing) return throwError(() => new Error('Example task not found'));
    const updated = {
      ...existing,
      ...body,
      updatedAt: new Date().toISOString(),
    };
    this.tasks.update((tasks) => tasks.map((t) => (t.id === id ? updated : t)));
    return of(updated);
  }
  createCalendarEvent(body: CreateCalendarEventRequest) {
    const now = new Date().toISOString();
    const event: CalendarEventDto = {
      ...body,
      id: crypto.randomUUID(),
      description: body.description ?? null,
      categoryId: body.categoryId ?? null,
      allDay: body.allDay ?? false,
      createdAt: now,
      updatedAt: now,
    };
    this.events.update((events) => [...events, event]);
    return of(event);
  }
  updateCalendarEvent(id: string, body: UpdateCalendarEventRequest) {
    const existing = this.events().find((e) => e.id === id);
    if (!existing)
      return throwError(() => new Error('Example event not found'));
    const updated = {
      ...existing,
      ...body,
      updatedAt: new Date().toISOString(),
    };
    this.events.update((events) =>
      events.map((e) => (e.id === id ? updated : e)),
    );
    return of(updated);
  }
}
