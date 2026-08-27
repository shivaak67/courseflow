import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { CategoryDto, GoalDto } from '../../core/api/api.models';

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
  readonly error = signal<string | null>(null);
  readonly goals = signal<GoalDto[]>([]);
  readonly categories = signal<CategoryDto[]>([]);

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: [''],
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
    const { title, categoryId } = this.form.getRawValue();
    this.api
      .createGoal({
        title,
        ...(categoryId ? { categoryId } : {}),
      })
      .subscribe({
        next: (created) => {
          this.goals.update((list) => [created, ...list]);
          this.form.reset({ title: '', categoryId: '' });
          this.saving.set(false);
        },
        error: () => {
          this.error.set('Could not create goal.');
          this.saving.set(false);
        },
      });
  }
}
