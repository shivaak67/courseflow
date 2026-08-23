import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Dashboard</mat-card-title>
        <mat-card-subtitle>Placeholder — summary UI comes later</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p>Due today, overdue, and workload widgets will land here.</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class DashboardComponent {}
