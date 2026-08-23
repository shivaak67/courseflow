import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Courses</mat-card-title>
        <mat-card-subtitle>Placeholder — course list comes later</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p>Manual and Canvas-synced courses will be managed here.</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class CoursesComponent {}
