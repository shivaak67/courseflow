import { Component, OnInit, inject, signal } from '@angular/core';
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

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    const { from, to } = lastSevenDaysWindow();
    this.api.getInsightsSummary(from, to).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load insights for the last 7 days.');
        this.summary.set(null);
        this.loading.set(false);
      },
    });
  }

  completionRatePercent(rate: number): string {
    const pct = rate <= 1 ? rate * 100 : rate;
    return `${Math.round(pct)}%`;
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

/** from = start of day 7 days ago (local), to = now, both as ISO Instant. */
function lastSevenDaysWindow(): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  from.setDate(from.getDate() - 7);
  return { from: from.toISOString(), to: to.toISOString() };
}
