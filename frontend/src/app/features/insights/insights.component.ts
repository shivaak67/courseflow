import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { InsightsSummary } from '../../core/api/api.models';

@Component({
  selector: 'app-insights',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './insights.component.html',
  styleUrl: './insights.component.scss',
})
export class InsightsComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly summary = signal<InsightsSummary | null>(null);

  readonly weeklyCompletionRate = computed(() => {
    const data = this.summary();
    if (!data || data.weeklyTasksDue === 0) {
      return 0;
    }
    return Math.round((data.weeklyTasksCompleted / data.weeklyTasksDue) * 100);
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    const { from, to } = thisWeekWindow();
    this.api.getInsightsSummary(from, to).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load insights for this week.');
        this.summary.set(null);
        this.loading.set(false);
      },
    });
  }

  formatFocusedMinutes(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours === 0) {
      return `${mins}m`;
    }
    if (mins === 0) {
      return `${hours}h`;
    }
    return `${hours}h ${mins}m`;
  }

  formatDay(date: string): string {
    const parsed = new Date(`${date}T12:00:00`);
    if (Number.isNaN(parsed.getTime())) {
      return date;
    }
    return parsed.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    });
  }
}

function thisWeekWindow(): { from: string; to: string } {
  const now = new Date();
  const day = now.getDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() + mondayOffset, 0, 0, 0, 0);
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekEnd.getDate() + 7);
  return { from: weekStart.toISOString(), to: weekEnd.toISOString() };
}
