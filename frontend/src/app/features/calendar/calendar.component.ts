import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { CalendarEventDto, TaskDto } from '../../core/api/api.models';

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
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss',
})
export class CalendarComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly tasks = signal<TaskDto[]>([]);
  readonly events = signal<CalendarEventDto[]>([]);
  readonly viewMonth = signal(startOfMonth(new Date()));

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

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const { from, to } = monthRangeIso(this.viewMonth());
    forkJoin({
      tasks: this.api.listTasks(),
      events: this.api.listCalendarEvents(from, to),
    }).subscribe({
      next: ({ tasks, events }) => {
        this.tasks.set(tasks);
        this.events.set(events);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load calendar data.');
        this.loading.set(false);
      },
    });
  }

  prevMonth(): void {
    const current = this.viewMonth();
    this.viewMonth.set(new Date(current.getFullYear(), current.getMonth() - 1, 1));
    this.reloadMonthEvents();
  }

  nextMonth(): void {
    const current = this.viewMonth();
    this.viewMonth.set(new Date(current.getFullYear(), current.getMonth() + 1, 1));
    this.reloadMonthEvents();
  }

  private reloadMonthEvents(): void {
    const { from, to } = monthRangeIso(this.viewMonth());
    this.api.listCalendarEvents(from, to).subscribe({
      next: (events) => this.events.set(events),
      error: () => this.error.set('Could not refresh month events.'),
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
  if (/^\d{4}-\d{2}-\d{2}$/.test(dueDate)) {
    return dueDate;
  }
  return toLocalDateKey(new Date(dueDate));
}

function mondayIndex(date: Date): number {
  return (date.getDay() + 6) % 7;
}

function monthRangeIso(viewMonth: Date): { from: string; to: string } {
  const from = new Date(viewMonth.getFullYear(), viewMonth.getMonth(), 1, 0, 0, 0, 0);
  const to = new Date(viewMonth.getFullYear(), viewMonth.getMonth() + 1, 1, 0, 0, 0, 0);
  return { from: from.toISOString(), to: to.toISOString() };
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
    if (!list.some((item) => item.id === chip.id)) {
      list.push(chip);
      byDay.set(key, list);
    }
  };

  for (const task of tasks) {
    if (!task.dueDate) {
      continue;
    }
    const key = dueDateKey(task.dueDate);
    push(key, { id: `task-${task.id}`, label: task.title, kind: 'task' });
  }

  for (const event of events) {
    const keys = eventDayKeys(event);
    for (const key of keys) {
      push(key, { id: `event-${event.id}-${key}`, label: event.title, kind: 'event' });
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

function eventDayKeys(event: CalendarEventDto): string[] {
  const keys = new Set<string>();
  keys.add(toLocalDateKey(new Date(event.startAt)));

  if (event.allDay) {
    let cursor = new Date(event.startAt);
    const endExclusive = new Date(event.endAt);
    while (true) {
      cursor = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate() + 1);
      if (cursor.getTime() >= endExclusive.getTime()) {
        break;
      }
      keys.add(toLocalDateKey(cursor));
    }
  }

  return [...keys];
}
