import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { CategoryDto, ProjectDto } from '../../core/api/api.models';

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
  readonly error = signal<string | null>(null);
  readonly projects = signal<ProjectDto[]>([]);
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
      projects: this.api.listProjects(),
      categories: this.api.listCategories(),
    }).subscribe({
      next: ({ projects, categories }) => {
        this.projects.set(projects);
        this.categories.set(categories);
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

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const { title, categoryId } = this.form.getRawValue();
    this.api
      .createProject({
        title,
        ...(categoryId ? { categoryId } : {}),
      })
      .subscribe({
        next: (created) => {
          this.projects.update((list) => [created, ...list]);
          this.form.reset({ title: '', categoryId: '' });
          this.saving.set(false);
        },
        error: () => {
          this.error.set('Could not create project.');
          this.saving.set(false);
        },
      });
  }
}
