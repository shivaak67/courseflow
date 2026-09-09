import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  template: ` <div class="public-page">
    <nav aria-label="Main navigation">
      <a class="brand" routerLink="/"
        >Prioritize<span>Make room for what matters.</span></a
      ><a routerLink="/auth/login">Sign in →</a>
    </nav>
    <main>
      <section class="hero">
        <div>
          <p class="eyebrow">LESS OVERWHELM. MORE FOLLOW-THROUGH.</p>
          <h1>
            A clear plan.<br />A little more<br /><em>breathing room.</em>
          </h1>
          <p class="intro">
            Bring your assignments, personal goals, and study time together.
            Choose what matters today and give it your full attention.
          </p>
          <div class="actions">
            <a class="primary" routerLink="/auth/register"
              >Start your own plan →</a
            ><a class="secondary" routerLink="/demo"
              >Try the interactive demo</a
            >
          </div>
          <p class="note">Explore the demo without creating an account.</p>
        </div>
        <div
          class="preview"
          aria-label="Example of a day planned in Prioritize"
        >
          <div class="preview-top">
            <span>YOUR DAY, WITH INTENTION</span
            ><span class="pill">Example plan</span>
          </div>
          <h2>One thing at a time.</h2>
          <p>Everything has a place. You have a starting point.</p>
          <div class="plan-row">
            <span class="time">9:00</span>
            <div>
              <strong>Make progress on your essay</strong
              ><small>45 minutes · Deep work</small>
            </div>
            <span>↗</span>
          </div>
          <div class="plan-row">
            <span class="time">10:00</span>
            <div>
              <strong>Review biology notes</strong
              ><small>30 minutes · Study</small>
            </div>
            <span>↗</span>
          </div>
          <div class="focus-preview">
            <span>YOUR NEXT FOCUS SESSION</span><strong>25:00</strong
            ><span>One task. A fresh start.</span>
          </div>
          <a routerLink="/demo">Make this example your own →</a>
        </div>
      </section>
      <section class="how">
        <p class="eyebrow">FROM “I SHOULD” TO “I DID”</p>
        <h2>Build momentum in three small steps.</h2>
        <div class="steps">
          <article>
            <span>01 / CAPTURE</span>
            <h3>Get it out of your head.</h3>
            <p>
              Add a task and a due date. Your guided first session helps you
              find a starting point.
            </p>
          </article>
          <article>
            <span>02 / PLAN</span>
            <h3>Give it a place in your day.</h3>
            <p>
              Pick a calendar day, add a time block, and adjust tasks and events
              as your plans change.
            </p>
          </article>
          <article>
            <span>03 / FOCUS</span>
            <h3>Start with one thing.</h3>
            <p>
              Choose your task, start the Focus timer, and track the time you
              actually spend.
            </p>
          </article>
        </div>
      </section>
      <section class="closing">
        <h2>Your next step can be a small one.</h2>
        <a class="primary" routerLink="/demo">Explore a sample day →</a>
      </section>
    </main>
    <footer>
      <span>Prioritize · A calmer way to plan</span>
      <div>
        <a routerLink="/privacy">Privacy</a><a routerLink="/terms">Terms</a>
      </div>
    </footer>
  </div>`,
  styleUrl: './public.scss',
})
export class LandingComponent {}
