import {
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { TaskDto } from '../../core/api/api.models';
import { OnboardingService } from './onboarding.service';
import { dateKey, localInput, validLocal } from '../../shared/planning-time';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.scss',
})
export class OnboardingComponent {
  readonly guide = inject(OnboardingService);
  private readonly api = inject(ApiService);
  readonly tasks = input<TaskDto[]>([]);
  readonly changed = output<void>();
  readonly busy = signal(false);
  readonly error = signal('');
  readonly createdTask = signal<TaskDto | null>(null);
  readonly task = computed(
    () =>
      this.tasks().find((t) => t.id === this.guide.state().taskId) ??
      this.createdTask(),
  );
  readonly visible = computed(
    () =>
      !this.guide.state().completed &&
      !this.guide.state().dismissed &&
      (this.tasks().length === 0 || this.guide.state().active),
  );
  readonly step = computed(() =>
    !this.task() ? 1 : !this.guide.state().eventId ? 2 : 3,
  );
  title = '';
  dueDate = dateKey(new Date());
  start = localInput(new Date(Date.now() + 60 * 60 * 1000));
  end = localInput(new Date(Date.now() + 90 * 60 * 1000));

  createTask(): void {
    if (this.busy()) return;
    this.error.set('');
    if (
      !this.title.trim() ||
      this.title.trim().length > 255 ||
      !validLocal(`${this.dueDate}T12:00`)
    ) {
      this.error.set('Enter a task title and a valid due date.');
      return;
    }
    this.busy.set(true);
    this.api
      .createTask({
        title: this.title.trim(),
        dueDate: this.dueDate,
        priority: 'MEDIUM',
        status: 'TODO',
      })
      .subscribe({
        next: (task) => {
          this.createdTask.set(task);
          this.guide.update({
            active: true,
            taskId: task.id,
            eventId: undefined,
          });
          this.busy.set(false);
          this.changed.emit();
        },
        error: () => {
          this.busy.set(false);
          this.error.set('Your task could not be saved. Please try again.');
        },
      });
  }
  schedule(): void {
    if (this.busy()) return;
    this.error.set('');
    const start = validLocal(this.start),
      end = validLocal(this.end),
      task = this.task();
    if (!start || !end || end <= start || !task) {
      this.error.set('Choose valid times with the end after the start.');
      return;
    }
    this.busy.set(true);
    this.api
      .createCalendarEvent({
        title: task.title,
        startAt: start.toISOString(),
        endAt: end.toISOString(),
        allDay: false,
      })
      .subscribe({
        next: (event) => {
          this.guide.update({ eventId: event.id });
          this.busy.set(false);
          this.changed.emit();
        },
        error: () => {
          this.busy.set(false);
          this.error.set(
            'Your time block could not be saved. Your task is safe; try again.',
          );
        },
      });
  }
}
