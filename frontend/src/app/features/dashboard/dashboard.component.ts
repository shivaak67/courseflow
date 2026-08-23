import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly highlights = [
    { label: 'Due today', value: '—', hint: 'Sync or add assignments' },
    { label: 'This week', value: '—', hint: 'Workload appears after import' },
    { label: 'Overdue', value: '—', hint: 'Nothing tracked yet' },
    { label: 'Hours left', value: '—', hint: 'Estimated remaining effort' },
  ];
}
