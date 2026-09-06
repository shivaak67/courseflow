import { Component } from '@angular/core';

@Component({
  selector: 'app-privacy',
  standalone: true,
  templateUrl: './privacy.component.html',
  styles: `
    .legal {
      max-width: 42rem;
      margin: 0 auto;
      padding: 2rem 1rem;
      line-height: 1.6;
    }
    a {
      color: var(--prio-teal-deep);
    }
    h1 {
      font-family: var(--prio-font-display);
    }
  `,
})
export class PrivacyComponent {}
