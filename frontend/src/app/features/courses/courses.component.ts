import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './courses.component.html',
  styleUrl: './courses.component.scss',
})
export class CoursesComponent {
  readonly samples = [
    { code: 'CS 350', name: 'Operating Systems', term: 'Fall 2026', load: 'Heavy' },
    { code: 'CS 440', name: 'Database Systems', term: 'Fall 2026', load: 'Steady' },
  ];
}
