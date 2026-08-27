import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { CategoryDto, TaskDto, TaskPriority, TaskStatus } from '../../core/api/api.models';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './tasks.component.html',
  styleUrl: './tasks.component.scss',
})
export class TasksComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly logging = signal(false);
  readonly error = signal<string | null>(null);
  readonly tasks = signal<TaskDto[]>([]);
  readonly categories = signal<CategoryDto[]>([]);

  readonly openTasks = computed(() => this.tasks().filter((t) => this.isOpen(t)));

  readonly priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
  readonly statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: [''],
    priority: ['MEDIUM' as TaskPriority, Validators.required],
    status: ['TODO' as TaskStatus, Validators.required],
  });

  readonly logForm = this.fb.nonNullable.group({
    taskId: ['', Validators.required],
    durationMinutes: [30, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      tasks: this.api.listTasks(),
      categories: this.api.listCategories(),
    }).subscribe({
      next: ({ tasks, categories }) => {
        this.tasks.set(tasks);
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load tasks.');
        this.loading.set(false);
      },
    });
  }

  categoryName(categoryId: string | null): string | null {
    if (!categoryId) {
      return null;
    }
    return this.categories().find((c) => c.id === categoryId)?.name ?? null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const { title, categoryId, priority, status } = this.form.getRawValue();
    this.api
      .createTask({
        title,
        priority,
        status,
        ...(categoryId ? { categoryId } : {}),
      })
      .subscribe({
        next: (created) => {
          this.tasks.update((list) => [created, ...list]);
          this.form.reset({ title: '', categoryId: '', priority: 'MEDIUM', status: 'TODO' });
          this.saving.set(false);
        },
        error: (err) => {
          const statusCode = err?.status as number | undefined;
          this.error.set(
            statusCode === 404
              ? 'Tasks API not available — restart the backend with the latest code.'
              : statusCode === 0
                ? 'Cannot reach the API. Is the backend running on port 8080?'
                : 'Could not create task.',
          );
          this.saving.set(false);
        },
      });
  }

  logTime(): void {
    if (this.logForm.invalid) {
      this.logForm.markAllAsTouched();
      return;
    }

    this.logging.set(true);
    this.error.set(null);
    const { taskId, durationMinutes } = this.logForm.getRawValue();
    this.api.createTimeEntry({ taskId, durationMinutes }).subscribe({
      next: () => {
        this.logForm.patchValue({ durationMinutes: 30 });
        this.logging.set(false);
        this.reload();
      },
      error: () => {
        this.error.set('Could not log time.');
        this.logging.set(false);
      },
    });
  }

  markComplete(task: TaskDto): void {
    if (task.status === 'COMPLETED') {
      return;
    }
    this.api
      .updateTask(task.id, {
        title: task.title,
        description: task.description,
        categoryId: task.categoryId,
        projectId: task.projectId,
        dueDate: task.dueDate,
        dueTime: task.dueTime,
        estimatedMinutes: task.estimatedMinutes,
        priority: task.priority,
        status: 'COMPLETED',
      })
      .subscribe({
        next: (updated) => {
          this.tasks.update((list) =>
            list.map((item) => (item.id === updated.id ? updated : item)),
          );
        },
        error: () => this.error.set('Could not update task.'),
      });
  }

  isOpen(task: TaskDto): boolean {
    return task.status !== 'COMPLETED' && task.status !== 'CANCELLED';
  }
}
