import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="legal prio-page">
      <a routerLink="/settings">← Back to Settings</a>
      <h1>Privacy Policy</h1>
      <p>
        Prioritize uses your phone number only to send SMS reminders you opt in to receive.
        We do not sell your personal information.
      </p>
      <p>Contact support for data requests or deletion.</p>
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
export class PrivacyComponent {}
