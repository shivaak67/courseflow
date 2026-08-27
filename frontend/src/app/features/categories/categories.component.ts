import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { CategoryDto } from '../../core/api/api.models';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.scss',
})
export class CategoriesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly renamingId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly categories = signal<CategoryDto[]>([]);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    color: [''],
    icon: [''],
  });

  readonly renameForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load categories.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const { name, color, icon } = this.form.getRawValue();
    this.api
      .createCategory({
        name: name.trim(),
        color: color.trim() || null,
        icon: icon.trim() || null,
      })
      .subscribe({
        next: (created) => {
          this.categories.update((list) => [created, ...list]);
          this.form.reset({ name: '', color: '', icon: '' });
          this.saving.set(false);
        },
        error: () => {
          this.error.set('Could not create category.');
          this.saving.set(false);
        },
      });
  }

  startRename(category: CategoryDto): void {
    this.renamingId.set(category.id);
    this.renameForm.reset({ name: category.name });
  }

  cancelRename(): void {
    this.renamingId.set(null);
  }

  saveRename(category: CategoryDto): void {
    if (this.renameForm.invalid) {
      this.renameForm.markAllAsTouched();
      return;
    }

    const name = this.renameForm.getRawValue().name.trim();
    if (!name || name === category.name) {
      this.renamingId.set(null);
      return;
    }

    this.error.set(null);
    this.api.updateCategory(category.id, { name }).subscribe({
      next: (updated) => {
        this.categories.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item)),
        );
        this.renamingId.set(null);
      },
      error: () => this.error.set('Could not rename category.'),
    });
  }

  remove(category: CategoryDto): void {
    this.error.set(null);
    this.api.deleteCategory(category.id).subscribe({
      next: () => {
        this.categories.update((list) => list.filter((item) => item.id !== category.id));
        if (this.renamingId() === category.id) {
          this.renamingId.set(null);
        }
      },
      error: () => this.error.set('Could not delete category.'),
    });
  }
}
