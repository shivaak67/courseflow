import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { CategoryDto, GoalDto, GoalStatus } from '../../core/api/api.models';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
})
export class GoalsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly goals = signal<GoalDto[]>([]);
  readonly categories = signal<CategoryDto[]>([]);

  readonly statuses: GoalStatus[] = ['ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: [''],
    description: [''],
    targetDate: [''],
    status: ['ACTIVE' as GoalStatus, Validators.required],
  });

  readonly editForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: [''],
    description: [''],
    targetDate: [''],
    status: ['ACTIVE' as GoalStatus, Validators.required],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      goals: this.api.listGoals(),
      categories: this.api.listCategories(),
    }).subscribe({
      next: ({ goals, categories }) => {
        this.goals.set(goals);
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load goals.');
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
    const payload = this.goalPayload(this.form.getRawValue());
    this.api.createGoal(payload).subscribe({
      next: (created) => {
        this.goals.update((list) => [created, ...list]);
        this.form.reset({
          title: '',
          categoryId: '',
          description: '',
          targetDate: '',
          status: 'ACTIVE',
        });
        this.saving.set(false);
      },
      error: () => {
        this.error.set('Could not create goal.');
        this.saving.set(false);
      },
    });
  }

  startEdit(goal: GoalDto): void {
    this.editingId.set(goal.id);
    this.editForm.reset({
      title: goal.title,
      categoryId: goal.categoryId ?? '',
      description: goal.description ?? '',
      targetDate: goal.targetDate ?? '',
      status: goal.status,
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  saveEdit(goal: GoalDto): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.error.set(null);
    const payload = this.goalPayload(this.editForm.getRawValue());
    this.api.updateGoal(goal.id, payload).subscribe({
      next: (updated) => {
        this.goals.update((list) => list.map((item) => (item.id === updated.id ? updated : item)));
        this.editingId.set(null);
      },
      error: () => this.error.set('Could not update goal.'),
    });
  }

  remove(goal: GoalDto): void {
    this.error.set(null);
    this.api.deleteGoal(goal.id).subscribe({
      next: () => {
        this.goals.update((list) => list.filter((item) => item.id !== goal.id));
        if (this.editingId() === goal.id) {
          this.editingId.set(null);
        }
      },
      error: () => this.error.set('Could not delete goal.'),
    });
  }

  private goalPayload(raw: {
    title: string;
    categoryId: string;
    description: string;
    targetDate: string;
    status: GoalStatus;
  }) {
    return {
      title: raw.title.trim(),
      categoryId: raw.categoryId || null,
      description: raw.description.trim() || null,
      targetDate: raw.targetDate || null,
      status: raw.status,
    };
  }
}
