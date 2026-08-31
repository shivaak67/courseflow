import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  CategoryDto,
  GoalDto,
  ProjectDto,
  ProjectStatus,
} from '../../core/api/api.models';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss',
})
export class ProjectsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly projects = signal<ProjectDto[]>([]);
  readonly categories = signal<CategoryDto[]>([]);
  readonly goals = signal<GoalDto[]>([]);

  readonly statuses: ProjectStatus[] = ['ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: [''],
    goalId: [''],
    description: [''],
    startDate: [''],
    targetDate: [''],
    status: ['ACTIVE' as ProjectStatus, Validators.required],
  });

  readonly editForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: [''],
    goalId: [''],
    description: [''],
    startDate: [''],
    targetDate: [''],
    status: ['ACTIVE' as ProjectStatus, Validators.required],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      projects: this.api.listProjects(),
      categories: this.api.listCategories(),
      goals: this.api.listGoals(),
    }).subscribe({
      next: ({ projects, categories, goals }) => {
        this.projects.set(projects);
        this.categories.set(categories);
        this.goals.set(goals);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load projects.');
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

  goalTitle(goalId: string | null): string | null {
    if (!goalId) {
      return null;
    }
    return this.goals().find((g) => g.id === goalId)?.title ?? null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const payload = this.projectPayload(this.form.getRawValue());
    this.api.createProject(payload).subscribe({
      next: (created) => {
        this.projects.update((list) => [created, ...list]);
        this.form.reset({
          title: '',
          categoryId: '',
          goalId: '',
          description: '',
          startDate: '',
          targetDate: '',
          status: 'ACTIVE',
        });
        this.saving.set(false);
      },
      error: () => {
        this.error.set('Could not create project.');
        this.saving.set(false);
      },
    });
  }

  startEdit(project: ProjectDto): void {
    this.editingId.set(project.id);
    this.editForm.reset({
      title: project.title,
      categoryId: project.categoryId ?? '',
      goalId: project.goalId ?? '',
      description: project.description ?? '',
      startDate: project.startDate ?? '',
      targetDate: project.targetDate ?? '',
      status: project.status,
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  saveEdit(project: ProjectDto): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.error.set(null);
    const payload = this.projectPayload(this.editForm.getRawValue());
    this.api.updateProject(project.id, payload).subscribe({
      next: (updated) => {
        this.projects.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item)),
        );
        this.editingId.set(null);
      },
      error: () => this.error.set('Could not update project.'),
    });
  }

  remove(project: ProjectDto): void {
    this.error.set(null);
    this.api.deleteProject(project.id).subscribe({
      next: () => {
        this.projects.update((list) => list.filter((item) => item.id !== project.id));
        if (this.editingId() === project.id) {
          this.editingId.set(null);
        }
      },
      error: () => this.error.set('Could not delete project.'),
    });
  }

  private projectPayload(raw: {
    title: string;
    categoryId: string;
    goalId: string;
    description: string;
    startDate: string;
    targetDate: string;
    status: ProjectStatus;
  }) {
    return {
      title: raw.title.trim(),
      categoryId: raw.categoryId || null,
      goalId: raw.goalId || null,
      description: raw.description.trim() || null,
      startDate: raw.startDate || null,
      targetDate: raw.targetDate || null,
      status: raw.status,
    };
  }
}
