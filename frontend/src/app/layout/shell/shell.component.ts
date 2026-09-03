import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard' },
    { label: 'Today', path: '/today', icon: 'timeline' },
    { label: 'Tasks', path: '/tasks', icon: 'checklist' },
    { label: 'Calendar', path: '/calendar', icon: 'calendar_month' },
    { label: 'Insights', path: '/insights', icon: 'bar_chart' },
    { label: 'Reminders', path: '/reminders', icon: 'notifications' },
    { label: 'Focus', path: '/focus', icon: 'center_focus_strong' },
    { label: 'Ask AI', path: '/assistant', icon: 'auto_awesome' },
  ];

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/auth/login');
  }
}
