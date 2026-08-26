import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { NotificationChannel, ReminderDto, TaskDto, UserProfileDto } from '../../core/api/api.models';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly savingProfile = signal(false);
  readonly savingSettings = signal(false);
  readonly savingReminder = signal(false);
  readonly sendingCode = signal(false);
  readonly verifyingPhone = signal(false);
  readonly error = signal<string | null>(null);
  readonly profileSaved = signal(false);
  readonly settingsSaved = signal(false);
  readonly codeSent = signal(false);

  readonly profile = signal<UserProfileDto | null>(null);
  readonly tasks = signal<TaskDto[]>([]);
  readonly reminders = signal<ReminderDto[]>([]);

  readonly openTasks = computed(() =>
    this.tasks().filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED'),
  );

  readonly pendingReminders = computed(() =>
    this.reminders()
      .filter((r) => r.status === 'PENDING')
      .sort((a, b) => new Date(a.reminderAt).getTime() - new Date(b.reminderAt).getTime()),
  );

  readonly phoneNeedsVerification = computed(() => {
    const p = this.profile();
    return !!p?.phoneNumber && !p.phoneVerified;
  });

  readonly profileForm = this.fb.nonNullable.group({
    timezone: [''],
    phoneNumber: [''],
  });

  readonly verifyForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(4)]],
  });

  readonly settingsForm = this.fb.nonNullable.group({
    inAppEnabled: [true],
    smsEnabled: [false],
    emailEnabled: [false],
  });

  readonly reminderForm = this.fb.nonNullable.group({
    taskId: ['', Validators.required],
    reminderLocal: ['', Validators.required],
    channel: ['IN_APP' as NotificationChannel, Validators.required],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.profileSaved.set(false);
    this.settingsSaved.set(false);
    this.codeSent.set(false);
    forkJoin({
      profile: this.api.getMe(),
      settings: this.api.getNotificationSettings(),
      reminders: this.api.listReminders(),
      tasks: this.api.listTasks(),
    }).subscribe({
      next: ({ profile, settings, reminders, tasks }) => {
        this.applyProfile(profile);
        this.settingsForm.patchValue({
          inAppEnabled: settings.inAppEnabled,
          smsEnabled: settings.smsEnabled,
          emailEnabled: settings.emailEnabled,
        });
        this.reminders.set(reminders);
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load settings or reminders.');
        this.loading.set(false);
      },
    });
  }

  saveProfile(): void {
    this.savingProfile.set(true);
    this.error.set(null);
    this.profileSaved.set(false);
    this.codeSent.set(false);

    const value = this.profileForm.getRawValue();
    const phoneNumber = value.phoneNumber.trim();
    this.api
      .updateMe({
        timezone: value.timezone.trim() || null,
        phoneNumber: phoneNumber || '',
      })
      .subscribe({
        next: (profile) => {
          this.applyProfile(profile);
          this.savingProfile.set(false);
          this.profileSaved.set(true);
        },
        error: () => {
          this.error.set('Could not save profile. Use E.164 format for phone (e.g. +15551234567).');
          this.savingProfile.set(false);
        },
      });
  }

  sendVerificationCode(): void {
    this.sendingCode.set(true);
    this.error.set(null);
    this.codeSent.set(false);
    this.api.requestPhoneVerification().subscribe({
      next: () => {
        this.sendingCode.set(false);
        this.codeSent.set(true);
      },
      error: () => {
        this.error.set('Could not send verification code. Check that SMS is configured on the server.');
        this.sendingCode.set(false);
      },
    });
  }

  verifyPhone(): void {
    if (this.verifyForm.invalid) {
      this.verifyForm.markAllAsTouched();
      return;
    }

    const { code } = this.verifyForm.getRawValue();
    this.verifyingPhone.set(true);
    this.error.set(null);
    this.api.verifyPhone(code.trim()).subscribe({
      next: () => {
        this.verifyingPhone.set(false);
        this.verifyForm.reset({ code: '' });
        this.codeSent.set(false);
        this.api.getMe().subscribe({
          next: (profile) => this.applyProfile(profile),
          error: () => this.error.set('Phone verified, but could not refresh profile.'),
        });
      },
      error: () => {
        this.error.set('Could not verify phone. Check the code and try again.');
        this.verifyingPhone.set(false);
      },
    });
  }

  saveSettings(): void {
    this.savingSettings.set(true);
    this.error.set(null);
    this.settingsSaved.set(false);
    const value = this.settingsForm.getRawValue();
    this.api
      .updateNotificationSettings({
        inAppEnabled: value.inAppEnabled,
        smsEnabled: value.smsEnabled,
        emailEnabled: value.emailEnabled,
      })
      .subscribe({
        next: (settings) => {
          this.settingsForm.patchValue({
            inAppEnabled: settings.inAppEnabled,
            smsEnabled: settings.smsEnabled,
            emailEnabled: settings.emailEnabled,
          });
          this.savingSettings.set(false);
          this.settingsSaved.set(true);
        },
        error: () => {
          this.error.set('Could not save notification settings.');
          this.savingSettings.set(false);
        },
      });
  }

  createReminder(): void {
    if (this.reminderForm.invalid) {
      this.reminderForm.markAllAsTouched();
      return;
    }

    const { taskId, reminderLocal, channel } = this.reminderForm.getRawValue();
    const reminderAt = localInputToIso(reminderLocal);
    if (!reminderAt) {
      this.error.set('Enter a valid reminder date and time.');
      return;
    }

    this.savingReminder.set(true);
    this.error.set(null);
    this.api
      .createReminder({
        relatedEntityType: 'TASK',
        relatedEntityId: taskId,
        reminderAt,
        channel,
      })
      .subscribe({
        next: (created) => {
          this.reminders.update((list) => [created, ...list]);
          this.reminderForm.reset({ taskId: '', reminderLocal: '', channel: 'IN_APP' });
          this.savingReminder.set(false);
        },
        error: () => {
          this.error.set('Could not create reminder.');
          this.savingReminder.set(false);
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

  taskTitle(reminder: ReminderDto): string {
    if (reminder.relatedEntityType !== 'TASK') {
      return `${reminder.relatedEntityType}`;
    }
    return this.tasks().find((t) => t.id === reminder.relatedEntityId)?.title ?? 'Task';
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

  private applyProfile(profile: UserProfileDto): void {
    this.profile.set(profile);
    this.profileForm.patchValue({
      timezone: profile.timezone ?? '',
      phoneNumber: profile.phoneNumber ?? '',
    });
  }
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
