import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthProvider, UserDto } from '../../../core/auth/auth.models';

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
    const params = this.route.snapshot.queryParamMap;
    const rawToken = params.get('token');
    if (!rawToken) {
      this.error.set('Missing sign-in token from Google. Please try again.');
      return;
    }

    const token = decodeURIComponent(rawToken).trim();
    const profile = this.userFromQueryParams(params);
    if (profile) {
      this.auth.setSession(token, profile);
      this.auth.refreshCurrentUser();
      void this.router.navigateByUrl('/dashboard');
      return;
    }

    const jwtProfile = this.auth.userFromAccessToken(token);
    if (jwtProfile) {
      this.auth.setSession(token, jwtProfile);
      this.auth.refreshCurrentUser();
      void this.router.navigateByUrl('/dashboard');
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

  private userFromQueryParams(params: { get(name: string): string | null }): UserDto | null {
    const userId = params.get('userId');
    const email = params.get('email');
    if (!userId || !email) {
      return null;
    }

    const authProvider = (params.get('authProvider') ?? 'GOOGLE') as AuthProvider;
    return {
      id: userId,
      email,
      firstName: params.get('firstName') ?? 'Google',
      lastName: params.get('lastName') ?? 'User',
      authProvider,
      phoneNumber: params.get('phoneNumber'),
      phoneVerified: params.get('phoneVerified') === 'true',
    };
  }
}
