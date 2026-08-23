import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { DashboardSummary, PrioritizedAssignment, WorkloadByCourse } from '../../core/api/api.models';

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
  readonly syncing = signal(false);
  readonly error = signal<string | null>(null);
  readonly syncMessage = signal<string | null>(null);
  readonly summary = signal<DashboardSummary | null>(null);
  readonly focusItems = signal<PrioritizedAssignment[]>([]);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.dashboardSummary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load dashboard summary.');
        this.loading.set(false);
      },
    });

    this.api.prioritizedAssignments().subscribe({
      next: (items) => this.focusItems.set(items.slice(0, 4)),
      error: () => this.focusItems.set([]),
    });
  }

  syncCanvas(): void {
    this.syncing.set(true);
    this.syncMessage.set(null);
    this.api.syncCanvas().subscribe({
      next: (result) => {
        this.syncing.set(false);
        this.syncMessage.set(
          `Synced ${result.coursesUpserted} courses and ${result.assignmentsUpserted} assignments.`,
        );
        this.reload();
      },
      error: () => {
        this.syncing.set(false);
        this.syncMessage.set('Canvas sync failed. Check CANVAS settings or enable mock mode.');
      },
    });
  }

  highlights() {
    const s = this.summary();
    if (!s) {
      return [
        { label: 'Due today', value: '—', hint: 'Loading…' },
        { label: 'This week', value: '—', hint: 'Loading…' },
        { label: 'Overdue', value: '—', hint: 'Loading…' },
        { label: 'Hours left', value: '—', hint: 'Loading…' },
      ];
    }
    return [
      { label: 'Due today', value: String(s.dueTodayCount), hint: 'Deadlines landing today' },
      { label: 'This week', value: String(s.dueThisWeekCount), hint: 'Open work in the next 7 days' },
      { label: 'Overdue', value: String(s.overdueCount), hint: 'Past due and still open' },
      {
        label: 'Hours left',
        value: String(s.estimatedHoursRemainingThisWeek),
        hint: 'Estimated effort due this week',
      },
    ];
  }

  barWidth(item: WorkloadByCourse, maxHours: number): string {
    if (maxHours <= 0) {
      return '8%';
    }
    const pct = Math.max(8, Math.round((item.estimatedHours / maxHours) * 100));
    return `${pct}%`;
  }

  maxWorkloadHours(): number {
    const items = this.summary()?.workloadByCourse ?? [];
    return items.reduce((max, item) => Math.max(max, item.estimatedHours), 0);
  }

  dueLabel(dueDate: string | null): string {
    if (!dueDate) {
      return 'No due date';
    }
    const due = new Date(dueDate);
    const now = Date.now();
    const days = Math.ceil((due.getTime() - now) / (1000 * 60 * 60 * 24));
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
}
