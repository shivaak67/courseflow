import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './auth-callback.component.html',
  styleUrl: './auth-callback.component.scss',
})
export class AuthCallbackComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.error.set('Missing sign-in token from Google. Please try again.');
      return;
    }

    this.auth.completeOAuthLogin(token).subscribe({
      next: () => {
        void this.router.navigateByUrl('/dashboard');
      },
      error: () => {
        this.auth.logout();
        this.error.set('Google sign-in succeeded, but we could not load your profile.');
      },
    });
  }
}
