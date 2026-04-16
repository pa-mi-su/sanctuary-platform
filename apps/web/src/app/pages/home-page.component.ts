import { Component } from '@angular/core';

@Component({
  selector: 'app-home-page',
  standalone: true,
  template: `
    <section class="top-actions">
      <button class="pill-button" type="button">
        <span class="pill-icon">◎</span>
        <span>About Sanctuary</span>
      </button>
      <button class="pill-button" type="button">
        <span class="pill-icon">⌘</span>
        <span>Language: English</span>
      </button>
    </section>

    <section class="welcome-card glass-card">
      <div class="logo-wrap">
        <img
          class="logo-image"
          src="sanctuary-logo-source.png"
          alt="Sanctuary logo"
        />
      </div>

      <h1>Welcome to<br />your sanctuary</h1>
      <p class="welcome-question">How do you want to connect with God?</p>
      <p class="welcome-copy">Prayer, liturgy, and saints in one calm place.</p>
    </section>

    <section class="quick-links">
      <article class="nav-card glass-card">
        <div class="nav-card__left">
          <span class="nav-icon saints">👥</span>
          <span>Saints</span>
        </div>
        <span class="nav-arrow">↗</span>
      </article>

      <article class="nav-card glass-card">
        <div class="nav-card__left">
          <span class="nav-icon novenas">📘</span>
          <span>Novenas</span>
        </div>
        <span class="nav-arrow">↗</span>
      </article>

      <article class="nav-card glass-card">
        <div class="nav-card__left">
          <span class="nav-icon prayers">🕯</span>
          <span>Prayers</span>
        </div>
        <span class="nav-arrow">↗</span>
      </article>
    </section>
  `,
})
export class HomePageComponent {}
