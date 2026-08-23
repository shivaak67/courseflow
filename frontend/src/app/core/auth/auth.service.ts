import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserDto,
} from './auth.models';

const TOKEN_KEY = 'prioritize_fake_token';
const USER_KEY = 'prioritize_fake_user';

/**
 * Stub auth service for the Angular shell.
 * UI/mock only — Agent A owns real JWT wiring. HttpClient methods are typed
 * to the API contract so they can be swapped in later without reshaping callers.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiBaseUrl}/api/auth`;

  readonly currentUser = signal<UserDto | null>(this.readStoredUser());

  isAuthenticated(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  }

  /** Mock login for shell demos — does not call the backend. */
  mockLogin(email = 'demo@prioritize.local'): void {
    const user: UserDto = {
      id: '00000000-0000-0000-0000-000000000001',
      firstName: 'Demo',
      lastName: 'User',
      email,
      authProvider: 'LOCAL',
      role: 'USER',
    };
    this.persist('mock-token', user);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
  }

  /** Typed stub — ready for Agent A; currently unused by placeholder UI. */
  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.api}/login`, body)
      .pipe(tap((res) => this.persist(res.accessToken, res.user)));
  }

  /** Typed stub — ready for Agent A; currently unused by placeholder UI. */
  register(body: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.api}/register`, body)
      .pipe(tap((res) => this.persist(res.accessToken, res.user)));
  }

  me(): Observable<UserDto> {
    const cached = this.currentUser();
    if (cached) {
      return of(cached);
    }
    return this.http.get<UserDto>(`${this.api}/me`);
  }

  private persist(token: string, user: UserDto): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private readStoredUser(): UserDto | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserDto;
    } catch {
      return null;
    }
  }
}
