import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'auth/register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: 'auth/callback',
    loadComponent: () =>
      import('./features/auth/callback/auth-callback.component').then(
        (m) => m.AuthCallbackComponent,
      ),
  },
  {
    path: 'privacy',
    loadComponent: () =>
      import('./features/legal/privacy.component').then((m) => m.PrivacyComponent),
  },
  {
    path: 'terms',
    loadComponent: () =>
      import('./features/legal/terms.component').then((m) => m.TermsComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent,
          ),
      },
      {
        path: 'today',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/today/today.component').then((m) => m.TodayComponent),
      },
      {
        path: 'tasks',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/tasks/tasks.component').then((m) => m.TasksComponent),
      },
      {
        path: 'calendar',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/calendar/calendar.component').then(
            (m) => m.CalendarComponent,
          ),
      },
      {
        path: 'focus',
        canActivate: [authGuard],
        canDeactivate: [(component: { canDeactivate: () => boolean }) => component.canDeactivate()],
        loadComponent: () =>
          import('./features/focus/focus.component').then((m) => m.FocusComponent),
      },
      {
        path: 'assistant',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/assistant/assistant.component').then(
            (m) => m.AssistantComponent,
          ),
      },
      {
        path: 'insights',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/insights/insights.component').then(
            (m) => m.InsightsComponent,
          ),
      },
      {
        path: 'reminders',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/reminders/reminders.component').then(
            (m) => m.RemindersComponent,
          ),
      },
      {
        path: 'settings',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/settings/settings.component').then(
            (m) => m.SettingsComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
