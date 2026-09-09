import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';

export interface GettingStartedState {
  active?: boolean;
  dismissed?: boolean;
  taskId?: string;
  eventId?: string;
  completed?: boolean;
}
@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly auth = inject(AuthService);
  private readonly revision = signal(0);
  private readonly memory = new Map<string, GettingStartedState>();
  private key(): string {
    return `prioritize.getting-started.v1.${this.auth.currentUser()?.id ?? 'guest'}`;
  }
  readonly state = computed((): GettingStartedState => {
    this.revision();
    const key = this.key();
    if (this.memory.has(key)) return this.memory.get(key)!;
    try {
      return JSON.parse(localStorage.getItem(key) ?? '{}') ?? {};
    } catch {
      return {};
    }
  });
  update(change: GettingStartedState): void {
    const next = { ...this.state(), ...change };
    this.memory.set(this.key(), next);
    try {
      localStorage.setItem(this.key(), JSON.stringify(next));
    } catch {
      /* The guide still works when storage is unavailable. */
    }
    this.revision.update((n) => n + 1);
  }
  open(): void {
    this.update({ active: true, dismissed: false, completed: false });
  }
  dismiss(): void {
    this.update({ active: false, dismissed: true });
  }
  startedFocus(taskId: string): void {
    if (this.state().taskId === taskId && this.state().eventId)
      this.update({ completed: true, active: false });
  }
}
