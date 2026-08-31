import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  DashboardSummary,
  InsightsSummary,
  NotificationDto,
  ScheduleBlockDto,
  TaskDto,
  TaskPriority,
} from '../../core/api/api.models';

interface DashMetric {
  label: string;
  value: string;
  hint: string;
  tone?: 'default' | 'warn' | 'accent';
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
  readonly completingId = signal<string | null>(null);
  readonly summary = signal<DashboardSummary | null>(null);
  readonly insights = signal<InsightsSummary | null>(null);
  readonly tasks = signal<TaskDto[]>([]);
  readonly notifications = signal<NotificationDto[]>([]);
  readonly todayBlocks = signal<ScheduleBlockDto[]>([]);

  readonly unreadCount = computed(() => this.notifications().length);

  readonly metrics = computed<DashMetric[]>(() => {
    const data = this.summary();
    const insight = this.insights();
    if (!data) {
      return [
        { label: 'Due today', value: '—', hint: 'Loading…' },
        { label: 'This week', value: '—', hint: 'Loading…' },
        { label: 'Overdue', value: '—', hint: 'Loading…' },
        { label: 'Open', value: '—', hint: 'Loading…' },
        { label: 'Minutes logged', value: '—', hint: 'Last 7 days' },
        { label: 'Completion', value: '—', hint: 'Last 7 days' },
      ];
    }

    return [
      {
        label: 'Due today',
        value: String(data.dueTodayCount),
        hint: 'Deadlines landing today',
        tone: data.dueTodayCount > 0 ? 'accent' : 'default',
      },
      {
        label: 'This week',
        value: String(data.dueThisWeekCount),
        hint: `${formatHours(data.estimatedHoursRemainingThisWeek)} estimated`,
      },
      {
        label: 'Overdue',
        value: String(data.overdueCount),
        hint: 'Past due and still open',
        tone: data.overdueCount > 0 ? 'warn' : 'default',
      },
      {
        label: 'Open',
        value: String(data.remainingCount),
        hint: `${data.highPriorityCount} high priority`,
      },
      {
        label: 'Minutes logged',
        value: String(insight?.totalMinutesLogged ?? 0),
        hint: 'Last 7 days',
      },
      {
        label: 'Completion',
        value: insight ? completionRatePercent(insight.completionRate) : '0%',
        hint: `${insight?.tasksCompleted ?? 0} tasks finished`,
      },
    ];
  });

  readonly workload = computed(() => this.summary()?.workloadByProject ?? []);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    const { from: insightsFrom, to: insightsTo } = lastSevenDaysWindow();
    const { from: todayFrom, to: todayTo } = todayRangeIso();

    forkJoin({
      summary: this.api.getDashboardSummary(),
      tasks: this.api.listTasks(),
      notifications: this.api.listNotifications(true),
      todayBlocks: this.api.listScheduleBlocks(todayFrom, todayTo),
      insights: this.api.getInsightsSummary(insightsFrom, insightsTo),
    }).subscribe({
      next: ({ summary, tasks, notifications, todayBlocks, insights }) => {
        this.summary.set(summary);
        this.tasks.set(tasks);
        this.notifications.set(notifications);
        this.todayBlocks.set(sortBlocks(todayBlocks));
        this.insights.set(insights);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load dashboard data. Is the backend running?');
        this.summary.set(null);
        this.insights.set(null);
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
      .slice(0, 5);
  }

  markComplete(task: TaskDto): void {
    if (task.status === 'COMPLETED' || this.completingId() === task.id) {
      return;
    }

    this.completingId.set(task.id);
    this.api
      .updateTask(task.id, {
        title: task.title,
        description: task.description,
        categoryId: task.categoryId,
        projectId: task.projectId,
        dueDate: task.dueDate,
        dueTime: task.dueTime,
        estimatedMinutes: task.estimatedMinutes,
        priority: task.priority,
        status: 'COMPLETED',
      })
      .subscribe({
        next: () => {
          this.completingId.set(null);
          this.reload();
        },
        error: () => {
          this.error.set('Could not mark task complete.');
          this.completingId.set(null);
        },
      });
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

  blockTitle(block: ScheduleBlockDto): string {
    return block.taskTitle ?? this.tasks().find((t) => t.id === block.taskId)?.title ?? 'Task';
  }

  formatBlockTime(block: ScheduleBlockDto): string {
    const start = new Date(block.startAt);
    const end = new Date(block.endAt);
    const time = (d: Date) =>
      d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
    return `${time(start)} – ${time(end)}`;
  }

  workloadBarWidth(hours: number): number {
    const items = this.workload();
    const max = Math.max(...items.map((w) => w.estimatedHours), 1);
    return Math.max(8, Math.round((hours / max) * 100));
  }
}

function completionRatePercent(rate: number): string {
  const pct = rate <= 1 ? rate * 100 : rate;
  return `${Math.round(pct)}%`;
}

function formatHours(hours: number): string {
  if (hours <= 0) {
    return '0h';
  }
  return `${hours}h`;
}

function sortBlocks(blocks: ScheduleBlockDto[]): ScheduleBlockDto[] {
  return [...blocks].sort(
    (a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime(),
  );
}

function todayRangeIso(): { from: string; to: string } {
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  const to = new Date(from);
  to.setDate(to.getDate() + 1);
  return { from: from.toISOString(), to: to.toISOString() };
}

function lastSevenDaysWindow(): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  from.setDate(from.getDate() - 7);
  return { from: from.toISOString(), to: to.toISOString() };
}
