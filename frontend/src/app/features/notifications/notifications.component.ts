import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { NotificationDto } from '../../core/api/api.models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
})
export class NotificationsComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly unreadOnly = signal(false);
  readonly notifications = signal<NotificationDto[]>([]);

  readonly unreadCount = computed(
    () => this.notifications().filter((n) => n.readAt == null).length,
  );

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listNotifications(this.unreadOnly()).subscribe({
      next: (items) => {
        this.notifications.set(
          [...items].sort(
            (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          ),
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load notifications.');
        this.loading.set(false);
      },
    });
  }

  onUnreadOnlyChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.unreadOnly.set(input.checked);
    this.reload();
  }

  isUnread(notification: NotificationDto): boolean {
    return notification.readAt == null;
  }

  markRead(notification: NotificationDto): void {
    if (!this.isUnread(notification)) {
      return;
    }
    this.api.markNotificationRead(notification.id).subscribe({
      next: (updated) => {
        if (this.unreadOnly()) {
          this.notifications.update((list) => list.filter((n) => n.id !== notification.id));
        } else {
          this.notifications.update((list) =>
            list.map((n) => (n.id === notification.id ? { ...n, ...updated } : n)),
          );
        }
      },
      error: () => this.error.set('Could not mark notification as read.'),
    });
  }

  delete(notification: NotificationDto): void {
    this.api.deleteNotification(notification.id).subscribe({
      next: () => {
        this.notifications.update((list) => list.filter((n) => n.id !== notification.id));
      },
      error: () => this.error.set('Could not delete notification.'),
    });
  }

  formatCreatedAt(iso: string): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return iso;
    }
    return date.toLocaleString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  }
}
