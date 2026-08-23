import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Calendar</mat-card-title>
        <mat-card-subtitle>Placeholder — due dates &amp; sessions come later</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p>Assignment due dates and study sessions will appear here.</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class CalendarComponent {}
