import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { forkJoin, switchMap } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule, RouterLink],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly savingSettings = signal(false);
  readonly enablingSms = signal(false);
  readonly disablingSms = signal(false);
  readonly sendingCode = signal(false);
  readonly verifyingCode = signal(false);
  readonly error = signal<string | null>(null);
  readonly settingsSaved = signal(false);
  readonly codeSent = signal(false);
  readonly phoneVerified = signal(false);
  readonly smsEnabled = signal(false);

  readonly settingsForm = this.fb.nonNullable.group({
    emailEnabled: [false],
  });

  readonly smsOptInForm = this.fb.nonNullable.group({
    phoneNumber: ['', Validators.required],
    consent: [false, Validators.requiredTrue],
  });

  readonly verifyForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
  });

  get canEnableSms(): boolean {
    return (
      this.smsOptInForm.controls.phoneNumber.valid &&
      this.smsOptInForm.controls.consent.value === true &&
      !this.enablingSms()
    );
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.settingsSaved.set(false);
    this.codeSent.set(false);

    forkJoin({
      settings: this.api.getNotificationSettings(),
      phone: this.api.getPhoneStatus(),
    }).subscribe({
      next: ({ settings, phone }) => {
        this.settingsForm.patchValue({
          emailEnabled: settings.emailEnabled,
        });
        this.smsEnabled.set(settings.smsEnabled);
        this.smsOptInForm.patchValue({
          phoneNumber: formatPhoneDisplay(phone.phoneNumber ?? ''),
          consent: false,
        });
        this.phoneVerified.set(phone.phoneVerified);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load settings.');
        this.loading.set(false);
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
        inAppEnabled: false,
        smsEnabled: this.smsEnabled(),
        emailEnabled: value.emailEnabled,
      })
      .subscribe({
        next: (settings) => {
          this.settingsForm.patchValue({
            emailEnabled: settings.emailEnabled,
          });
          this.smsEnabled.set(settings.smsEnabled);
          this.savingSettings.set(false);
          this.settingsSaved.set(true);
        },
        error: () => {
          this.error.set('Could not save notification settings.');
          this.savingSettings.set(false);
        },
      });
  }

  enableSmsReminders(): void {
    if (!this.canEnableSms) {
      this.smsOptInForm.markAllAsTouched();
      return;
    }

    const phoneNumber = normalizePhoneForApi(this.smsOptInForm.controls.phoneNumber.value);
    if (!phoneNumber) {
      this.error.set('Enter a valid US phone number.');
      return;
    }

    this.enablingSms.set(true);
    this.error.set(null);
    this.codeSent.set(false);
    this.phoneVerified.set(false);

    this.api
      .updatePhone({ phoneNumber })
      .pipe(
        switchMap(() =>
          this.api.updateNotificationSettings({
            inAppEnabled: false,
            smsEnabled: true,
            emailEnabled: this.settingsForm.controls.emailEnabled.value,
          }),
        ),
      )
      .subscribe({
        next: (settings) => {
          this.smsEnabled.set(settings.smsEnabled);
          this.smsOptInForm.patchValue({ consent: false });
          this.enablingSms.set(false);
          this.codeSent.set(true);
          this.auth.refreshCurrentUser();
        },
        error: () => {
          this.error.set('Could not enable SMS reminders.');
          this.enablingSms.set(false);
        },
      });
  }

  disableSmsReminders(): void {
    this.disablingSms.set(true);
    this.error.set(null);
    this.api
      .updateNotificationSettings({
        inAppEnabled: false,
        smsEnabled: false,
        emailEnabled: this.settingsForm.controls.emailEnabled.value,
      })
      .subscribe({
        next: (settings) => {
          this.smsEnabled.set(settings.smsEnabled);
          this.disablingSms.set(false);
        },
        error: () => {
          this.error.set('Could not disable SMS reminders.');
          this.disablingSms.set(false);
        },
      });
  }

  sendCode(): void {
    this.sendingCode.set(true);
    this.error.set(null);
    this.api.sendPhoneCode().subscribe({
      next: () => {
        this.sendingCode.set(false);
        this.codeSent.set(true);
      },
      error: () => {
        this.error.set('Could not send verification code.');
        this.sendingCode.set(false);
      },
    });
  }

  verifyCode(): void {
    if (this.verifyForm.invalid) {
      this.verifyForm.markAllAsTouched();
      return;
    }

    this.verifyingCode.set(true);
    this.error.set(null);
    this.api.verifyPhone({ code: this.verifyForm.controls.code.value }).subscribe({
      next: (phone) => {
        this.phoneVerified.set(phone.phoneVerified);
        this.verifyForm.reset({ code: '' });
        this.verifyingCode.set(false);
        this.auth.refreshCurrentUser();
      },
      error: () => {
        this.error.set('Invalid or expired verification code.');
        this.verifyingCode.set(false);
      },
    });
  }
}

function formatPhoneDisplay(raw: string): string {
  const digits = raw.replace(/\D/g, '');
  if (digits.length === 11 && digits.startsWith('1')) {
    return formatUsDigits(digits.slice(1));
  }
  if (digits.length === 10) {
    return formatUsDigits(digits);
  }
  return raw;
}

function formatUsDigits(digits: string): string {
  return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6, 10)}`;
}

function normalizePhoneForApi(raw: string): string | null {
  const digits = raw.replace(/\D/g, '');
  if (digits.length === 10) {
    return `+1${digits}`;
  }
  if (digits.length === 11 && digits.startsWith('1')) {
    return `+${digits}`;
  }
  if (raw.trim().startsWith('+') && /^\+\d{8,15}$/.test(raw.trim().replace(/[^\d+]/g, ''))) {
    return raw.trim().replace(/[^\d+]/g, '');
  }
  return null;
}
