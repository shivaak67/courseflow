import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly googleOAuthEnabled = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    this.auth.getConfig().subscribe({
      next: (config) => this.googleOAuthEnabled.set(config.googleOAuthEnabled),
      error: () => this.googleOAuthEnabled.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl('/dashboard');
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(this.toMessage(err));
      },
    });
  }

  continueWithGoogle(): void {
    this.auth.startGoogleSignIn();
  }

  private toMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const apiMessage = err.error?.message;
      if (typeof apiMessage === 'string' && apiMessage.length > 0) {
        return apiMessage;
      }
      if (err.status === 0) {
        return 'Cannot reach the API. Is the backend running?';
      }
      if (err.status === 401) {
        return 'Invalid email or password.';
      }
    }
    return 'Unable to log in. Please try again.';
  }
}
