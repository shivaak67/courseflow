import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  CalendarEventDto,
  ScheduleBlockDto,
  TaskDto,
} from '../../core/api/api.models';

export interface CalendarChip {
  id: string;
  label: string;
  kind: 'task' | 'event';
}

export interface CalendarCell {
  key: string;
  day: number | null;
  inMonth: boolean;
  isToday: boolean;
  chips: CalendarChip[];
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss',
})
export class CalendarComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly formSuccess = signal<string | null>(null);

  readonly tasks = signal<TaskDto[]>([]);
  readonly events = signal<CalendarEventDto[]>([]);
  readonly blocks = signal<ScheduleBlockDto[]>([]);
  readonly viewMonth = signal(startOfMonth(new Date()));
  readonly selectedDayKey = signal<string | null>(toLocalDateKey(new Date()));

  readonly eventForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    startLocal: ['', Validators.required],
    endLocal: ['', Validators.required],
  });

  readonly monthLabel = computed(() => {
    const d = this.viewMonth();
    return d.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  });

  readonly cells = computed(() =>
    buildMonthCells(this.viewMonth(), this.tasks(), this.events()),
  );

  readonly monthItemCount = computed(() =>
    this.cells().reduce((n, cell) => n + (cell.inMonth ? cell.chips.length : 0), 0),
  );

  readonly selectedDayLabel = computed(() => {
    const key = this.selectedDayKey();
    if (!key) {
      return 'Pick a day';
    }
    const [y, m, d] = key.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString(undefined, {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
    });
  });

  readonly dayTasks = computed(() => {
    const key = this.selectedDayKey();
    if (!key) {
      return [] as TaskDto[];
    }
    return this.tasks().filter((t) => t.dueDate && dueDateKey(t.dueDate) === key);
  });

  readonly dayEvents = computed(() => {
    const key = this.selectedDayKey();
    if (!key) {
      return [] as CalendarEventDto[];
    }
    return this.events().filter((e) => instantOverlapsDay(e.startAt, e.endAt, key));
  });

  readonly dayBlocks = computed(() => {
    const key = this.selectedDayKey();
    if (!key) {
      return [] as ScheduleBlockDto[];
    }
    return this.blocks().filter((b) => instantOverlapsDay(b.startAt, b.endAt, key));
  });

  readonly dayHasItems = computed(
    () =>
      this.dayTasks().length > 0 ||
      this.dayEvents().length > 0 ||
      this.dayBlocks().length > 0,
  );

  ngOnInit(): void {
    this.prefillEventTimesForDay(toLocalDateKey(new Date()));
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const { from, to } = monthRangeIso(this.viewMonth());
    forkJoin({
      tasks: this.api.listTasks(),
      events: this.api.listCalendarEvents(from, to),
      blocks: this.api.listScheduleBlocks(from, to),
    }).subscribe({
      next: ({ tasks, events, blocks }) => {
        this.tasks.set(tasks);
        this.events.set(events);
        this.blocks.set(blocks);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load calendar planning data.');
        this.loading.set(false);
      },
    });
  }

  prevMonth(): void {
    const current = this.viewMonth();
    this.viewMonth.set(new Date(current.getFullYear(), current.getMonth() - 1, 1));
    this.reloadMonthData();
  }

  nextMonth(): void {
    const current = this.viewMonth();
    this.viewMonth.set(new Date(current.getFullYear(), current.getMonth() + 1, 1));
    this.reloadMonthData();
  }

  selectDay(cell: CalendarCell): void {
    if (!cell.inMonth || cell.day == null) {
      return;
    }
    this.selectedDayKey.set(cell.key);
    this.prefillEventTimesForDay(cell.key);
  }

  submitEvent(): void {
    this.formError.set(null);
    this.formSuccess.set(null);

    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      return;
    }

    const { title, startLocal, endLocal } = this.eventForm.getRawValue();
    const startAt = localInputToIso(startLocal);
    const endAt = localInputToIso(endLocal);

    if (!startAt || !endAt) {
      this.formError.set('Enter valid start and end times.');
      return;
    }
    if (new Date(endAt).getTime() <= new Date(startAt).getTime()) {
      this.formError.set('End must be after start.');
      return;
    }

    this.submitting.set(true);
    this.api.createCalendarEvent({ title: title.trim(), startAt, endAt }).subscribe({
      next: (created) => {
        this.events.update((list) => [...list, created]);
        this.formSuccess.set('Event added.');
        this.eventForm.patchValue({ title: '' });
        this.submitting.set(false);
        const key = toLocalDateKey(new Date(created.startAt));
        this.selectedDayKey.set(key);
      },
      error: () => {
        this.formError.set('Could not save the event. Try again.');
        this.submitting.set(false);
      },
    });
  }

  deleteEvent(event: CalendarEventDto): void {
    this.api.deleteCalendarEvent(event.id).subscribe({
      next: () => {
        this.events.update((list) => list.filter((item) => item.id !== event.id));
      },
      error: () => this.error.set('Could not delete event.'),
    });
  }

  formatTimeRange(startAt: string, endAt: string): string {
    const start = new Date(startAt);
    const end = new Date(endAt);
    const time = (d: Date) =>
      d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
    return `${time(start)} – ${time(end)}`;
  }

  blockTitle(block: ScheduleBlockDto): string {
    if (block.taskTitle) {
      return block.taskTitle;
    }
    return this.tasks().find((t) => t.id === block.taskId)?.title ?? 'Scheduled task';
  }

  private reloadMonthData(): void {
    const { from, to } = monthRangeIso(this.viewMonth());
    forkJoin({
      events: this.api.listCalendarEvents(from, to),
      blocks: this.api.listScheduleBlocks(from, to),
    }).subscribe({
      next: ({ events, blocks }) => {
        this.events.set(events);
        this.blocks.set(blocks);
      },
      error: () => this.error.set('Could not refresh month events.'),
    });
  }

  private prefillEventTimesForDay(dayKey: string): void {
    const [y, m, d] = dayKey.split('-').map(Number);
    const start = new Date(y, m - 1, d, 9, 0, 0, 0);
    const end = new Date(y, m - 1, d, 10, 0, 0, 0);
    this.eventForm.patchValue({
      startLocal: toDatetimeLocalValue(start),
      endLocal: toDatetimeLocalValue(end),
    });
  }
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function toLocalDateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function dueDateKey(dueDate: string): string {
  // dueDate may be a plain date (YYYY-MM-DD) or an Instant.
  if (/^\d{4}-\d{2}-\d{2}$/.test(dueDate)) {
    return dueDate;
  }
  return toLocalDateKey(new Date(dueDate));
}

/** Monday = 0 … Sunday = 6 */
function mondayIndex(date: Date): number {
  return (date.getDay() + 6) % 7;
}

function monthRangeIso(viewMonth: Date): { from: string; to: string } {
  const from = new Date(viewMonth.getFullYear(), viewMonth.getMonth(), 1, 0, 0, 0, 0);
  const to = new Date(viewMonth.getFullYear(), viewMonth.getMonth() + 1, 1, 0, 0, 0, 0);
  return { from: from.toISOString(), to: to.toISOString() };
}

function localInputToIso(value: string): string | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
}

function toDatetimeLocalValue(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d}T${h}:${min}`;
}

function dayBounds(dayKey: string): { start: number; end: number } {
  const [y, m, d] = dayKey.split('-').map(Number);
  const start = new Date(y, m - 1, d, 0, 0, 0, 0).getTime();
  const end = new Date(y, m - 1, d + 1, 0, 0, 0, 0).getTime();
  return { start, end };
}

function instantOverlapsDay(startAt: string, endAt: string, dayKey: string): boolean {
  const { start: dayStart, end: dayEnd } = dayBounds(dayKey);
  const start = new Date(startAt).getTime();
  const end = new Date(endAt).getTime();
  return start < dayEnd && end > dayStart;
}

function buildMonthCells(
  viewMonth: Date,
  tasks: TaskDto[],
  events: CalendarEventDto[],
): CalendarCell[] {
  const year = viewMonth.getFullYear();
  const month = viewMonth.getMonth();
  const first = new Date(year, month, 1);
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const lead = mondayIndex(first);
  const todayKey = toLocalDateKey(new Date());

  const byDay = new Map<string, CalendarChip[]>();

  const push = (key: string, chip: CalendarChip): void => {
    const list = byDay.get(key) ?? [];
    list.push(chip);
    byDay.set(key, list);
  };

  for (const task of tasks) {
    if (!task.dueDate) {
      continue;
    }
    const key = dueDateKey(task.dueDate);
    push(key, { id: `task-${task.id}`, label: task.title, kind: 'task' });
  }

  for (const event of events) {
    const startKey = toLocalDateKey(new Date(event.startAt));
    push(startKey, { id: `event-${event.id}`, label: event.title, kind: 'event' });
    if (event.allDay) {
      let cursor = new Date(event.startAt);
      const endExclusive = new Date(event.endAt);
      while (true) {
        cursor = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate() + 1);
        if (cursor.getTime() >= endExclusive.getTime()) {
          break;
        }
        const key = toLocalDateKey(cursor);
        if (key !== startKey) {
          push(key, { id: `event-${event.id}-${key}`, label: event.title, kind: 'event' });
        }
      }
    }
  }

  const totalCells = Math.ceil((lead + daysInMonth) / 7) * 7;
  const cells: CalendarCell[] = [];

  for (let i = 0; i < totalCells; i++) {
    const dayNum = i - lead + 1;
    if (dayNum < 1 || dayNum > daysInMonth) {
      cells.push({
        key: `pad-${i}`,
        day: null,
        inMonth: false,
        isToday: false,
        chips: [],
      });
      continue;
    }
    const key = toLocalDateKey(new Date(year, month, dayNum));
    cells.push({
      key,
      day: dayNum,
      inMonth: true,
      isToday: key === todayKey,
      chips: byDay.get(key) ?? [],
    });
  }

  return cells;
}
