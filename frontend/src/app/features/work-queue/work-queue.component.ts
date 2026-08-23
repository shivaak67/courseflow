import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-work-queue',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Work Queue</mat-card-title>
        <mat-card-subtitle>Placeholder — prioritized list comes later</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p>“What should I work on?” will use /api/assignments/prioritized.</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class WorkQueueComponent {}
