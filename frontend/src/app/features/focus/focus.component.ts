import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { TaskDto, TimeEntryDto } from '../../core/api/api.models';

type FocusMode = 'timer' | 'stopwatch';
type FocusPhase = 'setup' | 'running' | 'paused' | 'complete';

interface FocusPreset {
  id: string;
  emoji: string;
  label: string;
  subtitle: string;
  minutes: number | null;
}

interface RecentSession {
  id: string;
  taskTitle: string;
  durationMinutes: number;
  when: string;
}

const PRESETS: FocusPreset[] = [
  { id: 'quick', emoji: '⚡', label: 'Quick Focus', subtitle: '25 min', minutes: 25 },
  { id: 'study', emoji: '📚', label: 'Study Session', subtitle: '50 min', minutes: 50 },
  { id: 'deep', emoji: '🧠', label: 'Deep Work', subtitle: '90 min', minutes: 90 },
  { id: 'custom', emoji: '✏️', label: 'Custom', subtitle: 'Set your own', minutes: null },
];

const QUICK_MINUTES = [25, 45, 60, 90];
const CUSTOM_STEP = 5;
const MIN_CUSTOM_MINUTES = 5;
const MAX_CUSTOM_MINUTES = 180;

@Component({
  selector: 'app-focus',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './focus.component.html',
  styleUrl: './focus.component.scss',
})
export class FocusComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);

  readonly presets = PRESETS;
  readonly quickMinutes = QUICK_MINUTES;
  readonly customStep = CUSTOM_STEP;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly tasks = signal<TaskDto[]>([]);
  readonly timeEntries = signal<TimeEntryDto[]>([]);

  readonly mode = signal<FocusMode>('timer');
  readonly selectedPresetId = signal('study');
  readonly selectedTaskId = signal('');
  readonly durationMinutes = signal(50);
  readonly phase = signal<FocusPhase>('setup');
  readonly secondsRemaining = signal(50 * 60);
  readonly secondsElapsed = signal(0);

  private sessionStartedAt: Date | null = null;
  private pausedAt: Date | null = null;
  private accumulatedPauseMs = 0;
  private timerId: ReturnType<typeof setInterval> | null = null;

  readonly targetDurationSeconds = computed(() => this.durationMinutes() * 60);

  readonly focusTasks = computed(() =>
    this.tasks()
      .filter((task) => task.status === 'TODO' || task.status === 'IN_PROGRESS')
      .sort((a, b) => a.title.localeCompare(b.title)),
  );

  readonly selectedTask = computed(() =>
    this.focusTasks().find((task) => task.id === this.selectedTaskId()) ?? null,
  );

  readonly isCustom = computed(() => this.selectedPresetId() === 'custom');

  readonly timerLabel = computed(() => {
    if (this.mode() === 'stopwatch') {
      return formatCountdown(this.secondsElapsed());
    }
    return formatCountdown(this.secondsRemaining());
  });

  readonly progressPercent = computed(() => {
    if (this.mode() === 'stopwatch') {
      return 0;
    }
    const total = this.targetDurationSeconds();
    if (total <= 0) {
      return 0;
    }
    return Math.min(100, Math.max(0, ((total - this.secondsRemaining()) / total) * 100));
  });

  readonly statusFooter = computed(() => {
    if (!this.sessionStartedAt || this.phase() === 'setup' || this.phase() === 'complete') {
      return '';
    }
    const started = formatClockTime(this.sessionStartedAt);
    if (this.mode() === 'stopwatch') {
      return `Started ${started}`;
    }
    const remaining = Math.max(0, Math.ceil(this.secondsRemaining() / 60));
    return `Started ${started} · ${remaining} min remaining`;
  });

  readonly recentSessions = computed((): RecentSession[] => {
    const titles = new Map(this.tasks().map((task) => [task.id, task.title]));
    return this.timeEntries().slice(0, 5).map((entry) => ({
      id: entry.id,
      taskTitle: titles.get(entry.taskId) ?? 'Task',
      durationMinutes: entry.durationMinutes,
      when: formatSessionWhen(entry.startedAt ?? entry.createdAt),
    }));
  });

  readonly canStart = computed(
    () =>
      this.phase() === 'setup' &&
      !!this.selectedTaskId() &&
      (this.mode() === 'stopwatch' || this.targetDurationSeconds() > 0) &&
      !this.saving(),
  );

  readonly isActive = computed(
    () => this.phase() === 'running' || this.phase() === 'paused',
  );

  ngOnInit(): void {
    this.reload();
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      tasks: this.api.listTasks(),
      timeEntries: this.api.listTimeEntries(),
    }).subscribe({
      next: ({ tasks, timeEntries }) => {
        this.tasks.set(tasks);
        this.timeEntries.set(timeEntries);
        this.loading.set(false);

        if (!this.selectedTaskId()) {
          const first = tasks.find(
            (task) => task.status === 'TODO' || task.status === 'IN_PROGRESS',
          );
          if (first) {
            this.selectedTaskId.set(first.id);
          }
        }
      },
      error: () => {
        this.error.set('Could not load focus data.');
        this.loading.set(false);
      },
    });
  }

  setMode(next: FocusMode): void {
    if (this.phase() !== 'setup') {
      return;
    }
    this.mode.set(next);
    this.success.set(null);
    if (next === 'stopwatch') {
      this.secondsElapsed.set(0);
    } else {
      this.syncRemainingToDuration();
    }
  }

  selectPreset(preset: FocusPreset): void {
    if (this.phase() !== 'setup' || this.mode() !== 'timer') {
      return;
    }
    this.selectedPresetId.set(preset.id);
    if (preset.minutes != null) {
      this.durationMinutes.set(preset.minutes);
      this.secondsRemaining.set(preset.minutes * 60);
    }
  }

  selectQuickMinutes(minutes: number): void {
    if (this.phase() !== 'setup' || this.mode() !== 'timer') {
      return;
    }
    this.selectedPresetId.set('custom');
    this.durationMinutes.set(minutes);
    this.secondsRemaining.set(minutes * 60);
  }

  adjustCustomMinutes(delta: number): void {
    if (this.phase() !== 'setup' || this.mode() !== 'timer') {
      return;
    }
    this.selectedPresetId.set('custom');
    const next = clampInt(
      this.durationMinutes() + delta,
      MIN_CUSTOM_MINUTES,
      MAX_CUSTOM_MINUTES,
    );
    this.durationMinutes.set(next);
    this.secondsRemaining.set(next * 60);
  }

  onTaskChange(taskId: string): void {
    if (this.phase() !== 'setup') {
      return;
    }
    this.selectedTaskId.set(taskId);
  }

  startSession(): void {
    if (!this.canStart()) {
      return;
    }

    this.success.set(null);
    this.error.set(null);
    this.sessionStartedAt = new Date();
    this.pausedAt = null;
    this.accumulatedPauseMs = 0;

    if (this.mode() === 'stopwatch') {
      this.secondsElapsed.set(0);
    } else {
      this.secondsRemaining.set(this.targetDurationSeconds());
    }

    this.phase.set('running');
    this.startTimer();
  }

  pauseSession(): void {
    if (this.phase() !== 'running') {
      return;
    }
    this.pausedAt = new Date();
    this.phase.set('paused');
    this.clearTimer();
    if (this.mode() === 'timer') {
      this.syncRemainingFromElapsed();
    } else {
      this.secondsElapsed.set(Math.floor(this.elapsedMs() / 1000));
    }
  }

  resumeSession(): void {
    if (this.phase() !== 'paused' || !this.pausedAt) {
      return;
    }
    this.accumulatedPauseMs += Date.now() - this.pausedAt.getTime();
    this.pausedAt = null;
    this.phase.set('running');
    this.startTimer();
  }

  stopAndSave(): void {
    if (this.phase() === 'setup' || this.phase() === 'complete') {
      return;
    }
    this.clearTimer();
    this.persistSession(this.mode() === 'timer' && this.secondsRemaining() <= 0);
  }

  startAnother(): void {
    this.resetSession();
    this.success.set(null);
  }

  private startTimer(): void {
    this.clearTimer();
    this.timerId = setInterval(() => this.tick(), 250);
  }

  private tick(): void {
    if (this.mode() === 'stopwatch') {
      this.secondsElapsed.set(Math.floor(this.elapsedMs() / 1000));
      return;
    }

    this.syncRemainingFromElapsed();
    if (this.secondsRemaining() <= 0) {
      this.secondsRemaining.set(0);
      this.clearTimer();
      this.persistSession(true);
    }
  }

  private syncRemainingFromElapsed(): void {
    const elapsedSeconds = Math.floor(this.elapsedMs() / 1000);
    const remaining = this.targetDurationSeconds() - elapsedSeconds;
    this.secondsRemaining.set(Math.max(0, remaining));
  }

  private syncRemainingToDuration(): void {
    if (this.phase() === 'setup' && this.mode() === 'timer') {
      this.secondsRemaining.set(this.targetDurationSeconds());
    }
  }

  private elapsedMs(): number {
    if (!this.sessionStartedAt) {
      return 0;
    }
    const end = this.pausedAt ?? new Date();
    return Math.max(0, end.getTime() - this.sessionStartedAt.getTime() - this.accumulatedPauseMs);
  }

  private persistSession(completed: boolean): void {
    const taskId = this.selectedTaskId();
    const startedAt = this.sessionStartedAt;
    if (!taskId || !startedAt) {
      return;
    }

    const endedAt = new Date();
    const elapsedMinutes = Math.max(1, Math.round(this.elapsedMs() / 60_000));
    const plannedMinutes = Math.max(1, Math.round(this.targetDurationSeconds() / 60));
    const durationMinutes =
      this.mode() === 'stopwatch'
        ? elapsedMinutes
        : completed
          ? plannedMinutes
          : Math.min(elapsedMinutes, plannedMinutes);

    this.saving.set(true);
    this.error.set(null);

    const notes =
      this.mode() === 'stopwatch'
        ? 'Focus session (stopwatch)'
        : completed
          ? 'Focus session (completed)'
          : 'Focus session';

    this.api
      .createTimeEntry({
        taskId,
        startedAt: startedAt.toISOString(),
        endedAt: endedAt.toISOString(),
        durationMinutes,
        notes,
      })
      .subscribe({
        next: (entry) => {
          this.timeEntries.update((entries) => [entry, ...entries]);
          this.phase.set('complete');
          this.saving.set(false);
          this.success.set(
            `Logged ${durationMinutes} minutes on “${this.selectedTask()?.title ?? 'task'}”.`,
          );
        },
        error: () => {
          this.saving.set(false);
          this.error.set('Could not save this focus session. Try again.');
          this.phase.set('setup');
          this.resetSession();
        },
      });
  }

  private resetSession(): void {
    this.clearTimer();
    this.sessionStartedAt = null;
    this.pausedAt = null;
    this.accumulatedPauseMs = 0;
    this.phase.set('setup');
    this.secondsElapsed.set(0);
    this.syncRemainingToDuration();
  }

  private clearTimer(): void {
    if (this.timerId != null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}

function clampInt(value: number, min: number, max: number): number {
  if (Number.isNaN(value)) {
    return min;
  }
  return Math.min(max, Math.max(min, Math.trunc(value)));
}

function formatCountdown(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }

  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

function formatClockTime(date: Date): string {
  return date.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
}

function formatSessionWhen(iso: string): string {
  const date = new Date(iso);
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}
