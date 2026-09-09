import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { CalendarComponent } from '../calendar/calendar.component';
import { DemoStore } from './demo.store';

@Component({
  selector: 'app-demo',
  standalone: true,
  imports: [FormsModule, RouterLink, CalendarComponent],
  providers: [DemoStore, { provide: ApiService, useExisting: DemoStore }],
  styleUrl: './public.scss',
  template: `<div class="public-page">
    <nav aria-label="Main navigation">
      <a class="brand" routerLink="/"
        >Prioritize<span>Make room for what matters.</span></a
      ><a routerLink="/auth/register">Create your own plan →</a>
    </nav>
    <main>
      <header class="demo-header">
        <p class="eyebrow">TAKE A LOOK AROUND</p>
        <h1>A small plan. A fresh start.</h1>
        <p>Check off a task, shape your calendar, or try a Focus session.</p>
      </header>
      <div class="demo-banner">
        You're exploring fictional example data. Changes stay in this demo and
        reset when you leave or reload. No account needed.
      </div>
      <div class="demo-tabs" aria-label="Demo views">
        @for (view of views; track view) {
          <button
            [class.active]="tab() === view"
            [attr.aria-pressed]="tab() === view"
            (click)="tab.set(view)"
          >
            {{ view }}
          </button>
        }
        <button (click)="reset()">Reset example</button>
      </div>
      <section class="demo-body">
        @switch (tab()) {
          @case ('Tasks') {
            <h2>A little progress today.</h2>
            <p class="demo-status" aria-live="polite">
              {{ completed() }} of {{ store.tasks().length }} tasks completed
            </p>
            @for (task of store.tasks(); track task.id) {
              <label class="task-row"
                ><input
                  type="checkbox"
                  [checked]="task.status === 'COMPLETED'"
                  (change)="toggle(task.id)"
                  [attr.aria-label]="'Complete ' + task.title"
                />
                <div [class.done]="task.status === 'COMPLETED'">
                  <strong>{{ task.title }}</strong
                  ><small
                    >Due {{ task.dueDate || 'anytime' }} · Sample task</small
                  >
                </div></label
              >
            }
            <form class="task-form" (ngSubmit)="addTask()">
              <input
                name="title"
                [(ngModel)]="title"
                required
                maxlength="255"
                aria-label="New sample task"
                placeholder="What would you like to work on?"
              /><button type="submit">Add sample task</button>
            </form>
          }
          @case ('Calendar') {
            <app-calendar />
          }
          @case ('Focus') {
            <div class="demo-focus">
              <p class="eyebrow">ONE THING AT A TIME</p>
              <h2>Make space to concentrate.</h2>
              <label for="focus-task">Your sample task</label
              ><select
                id="focus-task"
                [(ngModel)]="focusTask"
                [disabled]="running()"
              >
                @for (task of store.tasks(); track task.id) {
                  <option [value]="task.id">{{ task.title }}</option>
                }
              </select>
              <div class="timer" role="timer" aria-label="Time remaining">
                {{ timeLabel() }}
              </div>
              <button (click)="toggleTimer()">
                {{
                  running()
                    ? 'Pause'
                    : seconds() === 0
                      ? 'Start again'
                      : 'Start focus'
                }}</button
              ><button (click)="resetTimer()">Reset timer</button>
              <p class="demo-status">
                Demo timer only. Sign in to save your real focus time.
              </p>
            </div>
          }
        }
      </section>
    </main>
    <footer>
      <a routerLink="/">← Back to overview</a
      ><a routerLink="/auth/login">Already have an account? Sign in</a>
    </footer>
  </div>`,
})
export class DemoComponent implements OnDestroy {
  readonly store = inject(DemoStore);
  readonly views = ['Tasks', 'Calendar', 'Focus'] as const;
  readonly tab = signal<(typeof this.views)[number]>('Tasks');
  readonly completed = computed(
    () => this.store.tasks().filter((t) => t.status === 'COMPLETED').length,
  );
  readonly seconds = signal(25 * 60);
  readonly running = signal(false);
  readonly timeLabel = computed(
    () =>
      `${Math.floor(this.seconds() / 60)
        .toString()
        .padStart(
          2,
          '0',
        )}:${(this.seconds() % 60).toString().padStart(2, '0')}`,
  );
  title = '';
  focusTask = this.store.tasks()[0].id;
  private timer?: ReturnType<typeof setInterval>;
  private deadline = 0;
  addTask(): void {
    if (this.title.trim() && this.title.trim().length <= 255) {
      this.store.addTask(this.title.trim());
      this.title = '';
    }
  }
  toggle(id: string): void {
    const task = this.store.tasks().find((t) => t.id === id)!;
    this.store.updateTask(id, {
      status: task.status === 'COMPLETED' ? 'TODO' : 'COMPLETED',
    });
  }
  toggleTimer(): void {
    if (this.running()) {
      this.stopTimer();
      return;
    }
    if (!this.seconds()) this.seconds.set(25 * 60);
    this.deadline = Date.now() + this.seconds() * 1000;
    this.running.set(true);
    this.timer = setInterval(() => {
      this.seconds.set(
        Math.max(0, Math.ceil((this.deadline - Date.now()) / 1000)),
      );
      if (!this.seconds()) this.stopTimer();
    }, 250);
  }
  private stopTimer(): void {
    clearInterval(this.timer);
    this.running.set(false);
  }
  resetTimer(): void {
    this.stopTimer();
    this.seconds.set(25 * 60);
  }
  reset(): void {
    this.resetTimer();
    this.store.reset();
    this.focusTask = this.store.tasks()[0].id;
    this.title = '';
    this.tab.set('Tasks');
  }
  ngOnDestroy(): void {
    this.stopTimer();
  }
}
