import { Component } from '@angular/core';

@Component({
  selector: 'app-me-page',
  standalone: true,
  styleUrl: './me-page.component.scss',
  template: `
    <section class="screen-card me-screen glass-card">
      <div class="me-header">
        <h2>Me</h2>
        <p>Your novenas in progress and saved favorites.</p>
      </div>

      <section class="list-stack">
        <article class="panel-card glass-subtle">
          <h3>Novenas in Progress</h3>
          <p>0 in progress</p>
          <div class="divider"></div>
          <p>No novenas in progress.</p>
        </article>

        <article class="panel-card glass-subtle">
          <h3>Favorite Novenas</h3>
          <div class="divider"></div>
          <p>No favorite novenas yet.</p>
        </article>

        <article class="panel-card glass-subtle">
          <h3>Favorite Saints</h3>
          <div class="divider"></div>
          <p>No favorite saints yet.</p>
        </article>
      </section>
    </section>
  `,
})
export class MePageComponent {}
