import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  CalendarEventDto,
  NotificationChannel,
  ReminderDto,
  TaskDto,
} from '../../core/api/api.models';

export interface ReminderOffsetOption {
  minutes: number;
  label: string;
}

export interface ReminderGroup {
  key: string;
  title: string;
  eventAtLabel: string;
  reminders: ReminderDto[];
}

const OFFSET_OPTIONS: ReminderOffsetOption[] = [
  { minutes: 10_080, label: '1 week before' },
  { minutes: 1_440, label: '1 day before' },
  { minutes: 120, label: '2 hours before' },
  { minutes: 30, label: '30 minutes before' },
];

@Component({
  selector: 'app-reminders',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule, RouterLink],
  templateUrl: './reminders.component.html',
  styleUrl: './reminders.component.scss',
})
export class RemindersComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly offsetOptions = OFFSET_OPTIONS;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly saved = signal(false);

  readonly tasks = signal<TaskDto[]>([]);
  readonly events = signal<CalendarEventDto[]>([]);
  readonly reminders = signal<ReminderDto[]>([]);

  readonly scheduleForm = this.fb.nonNullable.group({
    entityKind: ['CALENDAR_EVENT' as 'CALENDAR_EVENT' | 'TASK', Validators.required],
    entityId: ['', Validators.required],
    emailEnabled: [true],
    smsEnabled: [false],
    offsets: this.fb.nonNullable.control<number[]>([1_440, 120], Validators.required),
  });

  readonly schedulableTasks = computed(() =>
    this.tasks().filter(
      (task) =>
        task.dueDate &&
        task.status !== 'COMPLETED' &&
        task.status !== 'CANCELLED',
    ),
  );

  readonly upcomingEvents = computed(() =>
    [...this.events()]
      .filter((event) => new Date(event.startAt).getTime() > Date.now())
      .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime()),
  );

  readonly pendingReminders = computed(() =>
    this.reminders()
      .filter((reminder) => reminder.status === 'PENDING')
      .sort((a, b) => new Date(a.reminderAt).getTime() - new Date(b.reminderAt).getTime()),
  );

  readonly reminderGroups = computed(() => this.groupReminders(this.pendingReminders()));

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.saved.set(false);
    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString();
    const to = new Date(now.getFullYear(), now.getMonth() + 3, 0, 23, 59, 59).toISOString();

    forkJoin({
      reminders: this.api.listReminders(),
      tasks: this.api.listTasks(),
      events: this.api.listCalendarEvents(from, to),
    }).subscribe({
      next: ({ reminders, tasks, events }) => {
        this.reminders.set(reminders);
        this.tasks.set(tasks);
        this.events.set(events);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load reminders.');
        this.loading.set(false);
      },
    });
  }

  onEntityKindChange(): void {
    this.scheduleForm.patchValue({ entityId: '' });
  }

  isOffsetSelected(minutes: number): boolean {
    return this.scheduleForm.controls.offsets.value.includes(minutes);
  }

  toggleOffset(minutes: number): void {
    const current = this.scheduleForm.controls.offsets.value;
    const next = current.includes(minutes)
      ? current.filter((value) => value !== minutes)
      : [...current, minutes].sort((a, b) => b - a);
    this.scheduleForm.controls.offsets.setValue(next);
    this.scheduleForm.controls.offsets.markAsTouched();
  }

  scheduleReminders(): void {
    if (this.scheduleForm.invalid) {
      this.scheduleForm.markAllAsTouched();
      return;
    }

    const value = this.scheduleForm.getRawValue();
    const channels: NotificationChannel[] = [];
    if (value.emailEnabled) {
      channels.push('EMAIL');
    }
    if (value.smsEnabled) {
      channels.push('SMS');
    }
    if (channels.length === 0) {
      this.error.set('Choose at least one channel: email or SMS.');
      return;
    }
    if (value.offsets.length === 0) {
      this.error.set('Choose at least one reminder time.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.saved.set(false);

    this.api
      .scheduleReminders({
        relatedEntityType: value.entityKind,
        relatedEntityId: value.entityId,
        offsetMinutes: value.offsets,
        channels,
      })
      .subscribe({
        next: (response) => {
          this.reminders.update((list) => [...response.reminders, ...list]);
          this.saving.set(false);
          this.saved.set(true);
        },
        error: () => {
          this.error.set('Could not schedule reminders.');
          this.saving.set(false);
        },
      });
  }

  cancelReminder(reminder: ReminderDto): void {
    this.api.cancelReminder(reminder.id).subscribe({
      next: (updated) => {
        this.reminders.update((list) =>
          list.map((item) =>
            item.id === reminder.id
              ? { ...item, ...(updated ?? {}), status: updated?.status ?? 'CANCELLED' }
              : item,
          ),
        );
      },
      error: () => this.error.set('Could not cancel reminder.'),
    });
  }

  offsetLabel(reminder: ReminderDto): string {
    const eventAt = this.resolveEventAt(reminder);
    if (!eventAt) {
      return this.formatReminderAt(reminder.reminderAt);
    }
    const diffMinutes = Math.round((eventAt.getTime() - new Date(reminder.reminderAt).getTime()) / 60_000);
    const preset = OFFSET_OPTIONS.find((option) => option.minutes === diffMinutes);
    if (preset) {
      return preset.label;
    }
    if (diffMinutes >= 1_440 && diffMinutes % 1_440 === 0) {
      const days = diffMinutes / 1_440;
      return `${days} day${days === 1 ? '' : 's'} before`;
    }
    if (diffMinutes >= 60 && diffMinutes % 60 === 0) {
      const hours = diffMinutes / 60;
      return `${hours} hour${hours === 1 ? '' : 's'} before`;
    }
    return `${diffMinutes} minutes before`;
  }

  channelLabel(channel: NotificationChannel): string {
    return channel === 'EMAIL' ? 'Email' : 'SMS';
  }

  formatReminderAt(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return iso;
    }
    return date.toLocaleString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  private groupReminders(reminders: ReminderDto[]): ReminderGroup[] {
    const groups = new Map<string, ReminderGroup>();
    for (const reminder of reminders) {
      const key = `${reminder.relatedEntityType}:${reminder.relatedEntityId}`;
      const title = this.entityTitle(reminder);
      const eventAt = this.resolveEventAt(reminder);
      const eventAtLabel = eventAt
        ? eventAt.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })
        : 'Scheduled item';

      const existing = groups.get(key);
      if (existing) {
        existing.reminders.push(reminder);
      } else {
        groups.set(key, {
          key,
          title,
          eventAtLabel,
          reminders: [reminder],
        });
      }
    }
    return [...groups.values()].sort((a, b) => {
      const aTime = this.resolveEventAt(a.reminders[0])?.getTime() ?? 0;
      const bTime = this.resolveEventAt(b.reminders[0])?.getTime() ?? 0;
      return aTime - bTime;
    });
  }

  private entityTitle(reminder: ReminderDto): string {
    if (reminder.relatedEntityType === 'TASK') {
      return this.tasks().find((task) => task.id === reminder.relatedEntityId)?.title ?? 'Task';
    }
    if (reminder.relatedEntityType === 'CALENDAR_EVENT') {
      return this.events().find((event) => event.id === reminder.relatedEntityId)?.title ?? 'Event';
    }
    return reminder.relatedEntityType;
  }

  private resolveEventAt(reminder: ReminderDto): Date | null {
    if (reminder.relatedEntityType === 'CALENDAR_EVENT') {
      const event = this.events().find((item) => item.id === reminder.relatedEntityId);
      return event ? new Date(event.startAt) : null;
    }
    if (reminder.relatedEntityType === 'TASK') {
      const task = this.tasks().find((item) => item.id === reminder.relatedEntityId);
      if (!task?.dueDate) {
        return null;
      }
      const time = task.dueTime ?? '09:00';
      return new Date(`${task.dueDate}T${time}`);
    }
    return null;
  }
}
