import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-work-queue',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './work-queue.component.html',
  styleUrl: './work-queue.component.scss',
})
export class WorkQueueComponent {
  readonly preview = [
    {
      title: 'Operating Systems Project',
      course: 'CS 350',
      due: 'Due in 2 days',
      level: 'HIGH',
      effort: '6h',
      reasons: ['Due soon', 'High point value', 'Marked HARD'],
    },
    {
      title: 'Database Homework 4',
      course: 'CS 440',
      due: 'Due in 4 days',
      level: 'MEDIUM',
      effort: '2h',
      reasons: ['Moderate urgency', 'Personal priority 4'],
    },
  ];
}
