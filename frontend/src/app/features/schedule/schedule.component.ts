import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { ScheduleBlockDto, TaskDto } from '../../core/api/api.models';

@Component({
  selector: 'app-schedule',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './schedule.component.html',
  styleUrl: './schedule.component.scss',
})
export class ScheduleComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly tasks = signal<TaskDto[]>([]);
  readonly blocks = signal<ScheduleBlockDto[]>([]);
  readonly weekAnchor = signal(startOfWeek(new Date()));

  readonly openTasks = computed(() =>
    this.tasks().filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED'),
  );

  readonly weekLabel = computed(() => {
    const start = this.weekAnchor();
    const end = endOfWeek(start);
    const fmt = (d: Date) =>
      d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
    return `${fmt(start)} – ${fmt(end)}`;
  });

  readonly form = this.fb.nonNullable.group({
    taskId: ['', Validators.required],
    startLocal: ['', Validators.required],
    endLocal: ['', Validators.required],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const { from, to } = weekRangeIso(this.weekAnchor());
    forkJoin({
      tasks: this.api.listTasks(),
      blocks: this.api.listScheduleBlocks(from, to),
    }).subscribe({
      next: ({ tasks, blocks }) => {
        this.tasks.set(tasks);
        this.blocks.set(sortBlocks(blocks));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load schedule or tasks.');
        this.loading.set(false);
      },
    });
  }

  prevWeek(): void {
    const current = this.weekAnchor();
    this.weekAnchor.set(addDays(current, -7));
    this.reloadBlocks();
  }

  nextWeek(): void {
    const current = this.weekAnchor();
    this.weekAnchor.set(addDays(current, 7));
    this.reloadBlocks();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { taskId, startLocal, endLocal } = this.form.getRawValue();
    const startAt = localInputToIso(startLocal);
    const endAt = localInputToIso(endLocal);

    if (!startAt || !endAt) {
      this.error.set('Enter valid start and end times.');
      return;
    }
    if (new Date(endAt).getTime() <= new Date(startAt).getTime()) {
      this.error.set('End must be after start.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.api.createScheduleBlock({ taskId, startAt, endAt }).subscribe({
      next: (created) => {
        this.blocks.update((list) => sortBlocks([created, ...list]));
        this.form.patchValue({ startLocal: '', endLocal: '' });
        this.saving.set(false);
      },
      error: () => {
        this.error.set('Could not create schedule block.');
        this.saving.set(false);
      },
    });
  }

  markComplete(block: ScheduleBlockDto): void {
    if (block.completed) {
      return;
    }
    this.api.updateScheduleBlock(block.id, {
      taskId: block.taskId,
      startAt: block.startAt,
      endAt: block.endAt,
      completed: true,
    }).subscribe({
      next: (updated) => {
        this.blocks.update((list) =>
          sortBlocks(list.map((item) => (item.id === updated.id ? updated : item))),
        );
      },
      error: () => this.error.set('Could not update schedule block.'),
    });
  }

  remove(block: ScheduleBlockDto): void {
    this.api.deleteScheduleBlock(block.id).subscribe({
      next: () => {
        this.blocks.update((list) => list.filter((item) => item.id !== block.id));
      },
      error: () => this.error.set('Could not delete schedule block.'),
    });
  }

  blockTitle(block: ScheduleBlockDto): string {
    if (block.taskTitle) {
      return block.taskTitle;
    }
    return this.tasks().find((t) => t.id === block.taskId)?.title ?? 'Task';
  }

  formatRange(block: ScheduleBlockDto): string {
    const start = new Date(block.startAt);
    const end = new Date(block.endAt);
    const day = start.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    });
    const time = (d: Date) =>
      d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
    return `${day} · ${time(start)} – ${time(end)}`;
  }

  private reloadBlocks(): void {
    this.loading.set(true);
    this.error.set(null);
    const { from, to } = weekRangeIso(this.weekAnchor());
    this.api.listScheduleBlocks(from, to).subscribe({
      next: (blocks) => {
        this.blocks.set(sortBlocks(blocks));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load schedule blocks.');
        this.loading.set(false);
      },
    });
  }
}

function sortBlocks(blocks: ScheduleBlockDto[]): ScheduleBlockDto[] {
  return [...blocks].sort(
    (a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime(),
  );
}

/** Monday 00:00 local as week start */
function startOfWeek(date: Date): Date {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const mondayOffset = (d.getDay() + 6) % 7;
  d.setDate(d.getDate() - mondayOffset);
  return d;
}

function endOfWeek(weekStart: Date): Date {
  return addDays(weekStart, 6);
}

function addDays(date: Date, days: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

function weekRangeIso(weekStart: Date): { from: string; to: string } {
  const from = new Date(weekStart);
  from.setHours(0, 0, 0, 0);
  const to = addDays(weekStart, 7);
  to.setHours(0, 0, 0, 0);
  return { from: from.toISOString(), to: to.toISOString() };
}

/** Convert datetime-local value to Instant ISO-8601 (Z). */
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
