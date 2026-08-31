import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="legal prio-page">
      <a routerLink="/settings">← Back to Settings</a>
      <h1>Terms &amp; Conditions</h1>
      <p>
        By enabling SMS reminders, you agree to receive automated texts from Prioritize about tasks
        and deadlines. Message and data rates may apply.
      </p>
      <p>Reply STOP to opt out or HELP for help.</p>
    </section>
  `,
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
export class TermsComponent {}
