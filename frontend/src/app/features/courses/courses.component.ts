import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { CourseDto } from '../../core/api/api.models';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './courses.component.html',
  styleUrl: './courses.component.scss',
})
export class CoursesComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly courses = signal<CourseDto[]>([]);

  ngOnInit(): void {
    this.api.listCourses().subscribe({
      next: (courses) => {
        this.courses.set(courses);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load courses.');
        this.loading.set(false);
      },
    });
  }
}
