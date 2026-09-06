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
        Prioritize uses your phone number to verify your number and send the task and calendar
        reminders you opt in to receive. Your number and message content are processed by our
        messaging provider, Twilio, to deliver these texts.
        We do not sell your personal information or share mobile numbers or SMS opt-in consent
        with third parties or affiliates for marketing or promotional purposes.
      </p>
      <p>Message frequency varies based on the reminders you schedule. Message and data rates may apply.</p>
      <p>You can disable SMS reminders in Settings or reply STOP to opt out. Reply HELP for help.</p>
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
