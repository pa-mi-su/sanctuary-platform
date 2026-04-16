import { Component } from '@angular/core';

@Component({
  selector: 'app-me-page',
  standalone: true,
  template: `
    <section class="screen-card me-screen glass-card">
      <div class="me-header">
        <h1>Me</h1>
        <p>Your novenas in progress and saved favorites.</p>
      </div>

      <div class="list-stack">
        <article class="panel-card glass-subtle">
          <h2>Novenas in Progress</h2>
          <p>0 in progress</p>
          <div class="divider"></div>
          <p>No novenas in progress.</p>
        </article>

        <article class="panel-card glass-subtle">
          <h2>Favorite Novenas</h2>
          <div class="divider"></div>
          <p>No favorite novenas yet.</p>
        </article>

        <article class="panel-card glass-subtle">
          <h2>Favorite Saints</h2>
          <div class="divider"></div>
          <p>No favorite saints yet.</p>
        </article>
      </div>
    </section>
  `,
})
export class MePageComponent {}
