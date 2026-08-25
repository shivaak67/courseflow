import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { TaskDto, TaskPriority } from '../../core/api/api.models';

interface DashMetric {
  label: string;
  value: string;
  hint: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly tasks = signal<TaskDto[]>([]);
  readonly metrics = signal<DashMetric[]>([
    { label: 'Due today', value: '—', hint: 'Loading…' },
    { label: 'This week', value: '—', hint: 'Loading…' },
    { label: 'Overdue', value: '—', hint: 'Loading…' },
    { label: 'Open', value: '—', hint: 'Loading…' },
  ]);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listTasks().subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.metrics.set(this.computeMetrics(tasks));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load tasks for dashboard metrics.');
        this.metrics.set([
          { label: 'Due today', value: '0', hint: 'Deadlines landing today' },
          { label: 'This week', value: '0', hint: 'Open work in the next 7 days' },
          { label: 'Overdue', value: '0', hint: 'Past due and still open' },
          { label: 'Open', value: '0', hint: 'Todo or in progress' },
        ]);
        this.loading.set(false);
      },
    });
  }

  focusItems(): TaskDto[] {
    const priorityRank: Record<TaskPriority, number> = {
      URGENT: 0,
      HIGH: 1,
      MEDIUM: 2,
      LOW: 3,
    };
    return this.tasks()
      .filter((t) => t.status === 'TODO' || t.status === 'IN_PROGRESS')
      .sort((a, b) => {
        const p = priorityRank[a.priority] - priorityRank[b.priority];
        if (p !== 0) {
          return p;
        }
        return (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999');
      })
      .slice(0, 4);
  }

  dueLabel(dueDate: string | null): string {
    if (!dueDate) {
      return 'No due date';
    }
    const due = new Date(`${dueDate}T12:00:00`);
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

  private computeMetrics(tasks: TaskDto[]): DashMetric[] {
    const today = this.toDateKey(new Date());
    const weekEnd = new Date();
    weekEnd.setDate(weekEnd.getDate() + 7);
    const weekEndKey = this.toDateKey(weekEnd);

    let dueToday = 0;
    let dueThisWeek = 0;
    let overdue = 0;
    let open = 0;

    for (const task of tasks) {
      const isOpen = task.status === 'TODO' || task.status === 'IN_PROGRESS';
      if (isOpen) {
        open += 1;
      }
      if (!task.dueDate || !isOpen) {
        continue;
      }
      if (task.dueDate === today) {
        dueToday += 1;
      }
      if (task.dueDate < today) {
        overdue += 1;
      } else if (task.dueDate <= weekEndKey) {
        dueThisWeek += 1;
      }
    }

    return [
      { label: 'Due today', value: String(dueToday), hint: 'Deadlines landing today' },
      { label: 'This week', value: String(dueThisWeek), hint: 'Open work in the next 7 days' },
      { label: 'Overdue', value: String(overdue), hint: 'Past due and still open' },
      { label: 'Open', value: String(open), hint: 'Todo or in progress' },
    ];
  }

  private toDateKey(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
