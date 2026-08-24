import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AssignmentDto, StudySessionDto } from '../../core/api/api.models';

export interface CalendarCell {
  key: string;
  day: number | null;
  inMonth: boolean;
  isToday: boolean;
  assignments: AssignmentDto[];
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss',
})
export class CalendarComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly formSuccess = signal<string | null>(null);

  readonly assignments = signal<AssignmentDto[]>([]);
  readonly sessions = signal<StudySessionDto[]>([]);
  readonly viewMonth = signal(startOfMonth(new Date()));
  readonly selectedDayKey = signal<string | null>(null);
  readonly selectedAssignmentId = signal<string>('');
  readonly durationMinutes = signal(60);
  readonly notes = signal('');

  readonly monthLabel = computed(() => {
    const d = this.viewMonth();
    return d.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  });

  readonly openAssignments = computed(() =>
    this.assignments().filter((a) => !a.completed && !a.submitted),
  );

  readonly cells = computed(() => buildMonthCells(this.viewMonth(), this.assignments()));

  readonly dueThisMonthCount = computed(() =>
    this.cells().reduce((n, cell) => n + (cell.inMonth ? cell.assignments.length : 0), 0),
  );

  readonly recentSessions = computed(() =>
    [...this.sessions()].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    ),
  );

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      assignments: this.api.listAssignments(),
      sessions: this.api.listStudySessions(),
    }).subscribe({
      next: ({ assignments, sessions }) => {
        this.assignments.set(assignments);
        this.sessions.set(sessions);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load calendar or study sessions.');
        this.loading.set(false);
      },
    });
  }

  prevMonth(): void {
    const current = this.viewMonth();
    this.viewMonth.set(new Date(current.getFullYear(), current.getMonth() - 1, 1));
  }

  nextMonth(): void {
    const current = this.viewMonth();
    this.viewMonth.set(new Date(current.getFullYear(), current.getMonth() + 1, 1));
  }

  selectDay(cell: CalendarCell): void {
    if (!cell.inMonth || cell.day == null) {
      return;
    }
    this.selectedDayKey.set(cell.key);
    if (cell.assignments.length > 0) {
      const open = cell.assignments.find((a) => !a.completed && !a.submitted) ?? cell.assignments[0];
      this.selectedAssignmentId.set(open.id);
    }
  }

  selectAssignment(assignment: AssignmentDto, event?: Event): void {
    event?.stopPropagation();
    this.selectedAssignmentId.set(assignment.id);
    if (assignment.dueDate) {
      this.selectedDayKey.set(toLocalDateKey(new Date(assignment.dueDate)));
      const due = new Date(assignment.dueDate);
      this.viewMonth.set(startOfMonth(due));
    }
  }

  submitSession(): void {
    this.formError.set(null);
    this.formSuccess.set(null);

    const assignmentId = this.selectedAssignmentId();
    const duration = Number(this.durationMinutes());

    if (!assignmentId) {
      this.formError.set('Choose an assignment to log time against.');
      return;
    }
    if (!Number.isFinite(duration) || duration < 1) {
      this.formError.set('Duration must be at least 1 minute.');
      return;
    }

    const notes = this.notes().trim();
    this.submitting.set(true);
    this.api
      .createStudySession({
        assignmentId,
        durationMinutes: Math.round(duration),
        notes: notes.length > 0 ? notes : null,
      })
      .subscribe({
        next: (session) => {
          this.sessions.update((list) => [session, ...list]);
          this.notes.set('');
          this.durationMinutes.set(60);
          this.formSuccess.set('Study session logged.');
          this.submitting.set(false);
        },
        error: () => {
          this.formError.set('Could not save the study session. Try again.');
          this.submitting.set(false);
        },
      });
  }

  sessionWhen(session: StudySessionDto): string {
    const raw = session.startedAt ?? session.createdAt;
    return new Date(raw).toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
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

/** Monday = 0 … Sunday = 6 */
function mondayIndex(date: Date): number {
  return (date.getDay() + 6) % 7;
}

function buildMonthCells(viewMonth: Date, assignments: AssignmentDto[]): CalendarCell[] {
  const year = viewMonth.getFullYear();
  const month = viewMonth.getMonth();
  const first = new Date(year, month, 1);
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const lead = mondayIndex(first);
  const todayKey = toLocalDateKey(new Date());

  const byDay = new Map<string, AssignmentDto[]>();
  for (const assignment of assignments) {
    if (!assignment.dueDate) {
      continue;
    }
    const key = toLocalDateKey(new Date(assignment.dueDate));
    const list = byDay.get(key) ?? [];
    list.push(assignment);
    byDay.set(key, list);
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
        assignments: [],
      });
      continue;
    }
    const key = toLocalDateKey(new Date(year, month, dayNum));
    cells.push({
      key,
      day: dayNum,
      inMonth: true,
      isToday: key === todayKey,
      assignments: byDay.get(key) ?? [],
    });
  }

  return cells;
}
