import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { PrioritizedAssignment } from '../../core/api/api.models';

@Component({
  selector: 'app-work-queue',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './work-queue.component.html',
  styleUrl: './work-queue.component.scss',
})
export class WorkQueueComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly items = signal<PrioritizedAssignment[]>([]);

  ngOnInit(): void {
    this.api.prioritizedAssignments().subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load prioritized assignments.');
        this.loading.set(false);
      },
    });
  }

  dueLabel(dueDate: string | null): string {
    if (!dueDate) {
      return 'No due date';
    }
    const due = new Date(dueDate);
    const days = Math.ceil((due.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
    if (days < 0) {
      return `Overdue by ${Math.abs(days)}d`;
    }
    if (days === 0) {
      return 'Due today';
    }
    if (days === 1) {
      return 'Due tomorrow';
    }
    return `Due in ${days} days`;
  }

  effortLabel(hours: number | null): string {
    if (hours == null) {
      return 'Effort TBD';
    }
    return `${hours}h`;
  }
}
