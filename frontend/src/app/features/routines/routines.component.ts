import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  RecurrenceType,
  RoutineDto,
  RoutineOccurrenceDto,
} from '../../core/api/api.models';

@Component({
  selector: 'app-routines',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './routines.component.html',
  styleUrl: './routines.component.scss',
})
export class RoutinesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly recurrenceTypes: RecurrenceType[] = [
    'DAILY',
    'WEEKLY',
    'SELECTED_WEEKDAYS',
    'MONTHLY',
  ];

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly routines = signal<RoutineDto[]>([]);
  readonly occurrences = signal<RoutineOccurrenceDto[]>([]);

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    recurrenceType: ['WEEKLY' as RecurrenceType, Validators.required],
    daysOfWeek: [''],
    startTime: ['09:00', Validators.required],
    endTime: [''],
    startDate: [toLocalDateKey(new Date()), Validators.required],
    endDate: [''],
    active: [true],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const { from, to } = nextDaysRange(14);
    forkJoin({
      routines: this.api.listRoutines(),
      occurrences: this.api.listRoutineOccurrences(from, to),
    }).subscribe({
      next: ({ routines, occurrences }) => {
        this.routines.set(routines);
        this.occurrences.set(sortOccurrences(occurrences));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load routines.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const needsDays =
      value.recurrenceType === 'WEEKLY' || value.recurrenceType === 'SELECTED_WEEKDAYS';
    const daysOfWeek = value.daysOfWeek.trim() || null;
    if (needsDays && !daysOfWeek) {
      this.error.set('Enter days of week (Mon=1 … Sun=7), e.g. 1,3,5.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.api
      .createRoutine({
        title: value.title.trim(),
        recurrenceType: value.recurrenceType,
        daysOfWeek: needsDays ? daysOfWeek : null,
        startTime: normalizeTime(value.startTime),
        endTime: value.endTime ? normalizeTime(value.endTime) : null,
        startDate: value.startDate,
        endDate: value.endDate || null,
        active: value.active,
        intervalValue: 1,
      })
      .subscribe({
        next: () => {
          this.form.reset({
            title: '',
            recurrenceType: 'WEEKLY',
            daysOfWeek: '',
            startTime: '09:00',
            endTime: '',
            startDate: toLocalDateKey(new Date()),
            endDate: '',
            active: true,
          });
          this.saving.set(false);
          this.reload();
        },
        error: () => {
          this.error.set('Could not create routine.');
          this.saving.set(false);
        },
      });
  }

  remove(routine: RoutineDto): void {
    this.api.deleteRoutine(routine.id).subscribe({
      next: () => this.reload(),
      error: () => this.error.set('Could not delete routine.'),
    });
  }

  formatOccurrence(occ: RoutineOccurrenceDto): string {
    const [y, m, d] = occ.date.split('-').map(Number);
    const day = new Date(y, m - 1, d).toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    });
    const time = formatClock(occ.startTime);
    const end = occ.endTime ? ` – ${formatClock(occ.endTime)}` : '';
    return `${day} · ${time}${end}`;
  }

  summary(routine: RoutineDto): string {
    const days = routine.daysOfWeek ? ` · days ${routine.daysOfWeek}` : '';
    const end = routine.endDate ? ` · until ${routine.endDate}` : '';
    return `${routine.recurrenceType}${days} · ${formatClock(routine.startTime)}${end}`;
  }
}

function toLocalDateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function nextDaysRange(days: number): { from: string; to: string } {
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  const to = new Date(from);
  to.setDate(to.getDate() + days);
  return { from: toLocalDateKey(from), to: toLocalDateKey(to) };
}

function normalizeTime(value: string): string {
  if (!value) {
    return value;
  }
  return value.length === 5 ? `${value}:00` : value;
}

function formatClock(value: string): string {
  if (!value) {
    return '';
  }
  const parts = value.split(':');
  return parts.length >= 2 ? `${parts[0]}:${parts[1]}` : value;
}

function sortOccurrences(items: RoutineOccurrenceDto[]): RoutineOccurrenceDto[] {
  return [...items].sort((a, b) => {
    const dateCmp = a.date.localeCompare(b.date);
    if (dateCmp !== 0) {
      return dateCmp;
    }
    return a.startTime.localeCompare(b.startTime);
  });
}
