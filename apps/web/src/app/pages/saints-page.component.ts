import { Component } from '@angular/core';

@Component({
  selector: 'app-saints-page',
  standalone: true,
  template: `
    <section class="screen-card glass-card">
      <div class="screen-header split">
        <button class="circle-button" type="button">‹</button>
        <div class="screen-title">
          <h1>April 13, 2026</h1>
          <p>Saints • Tap to jump</p>
        </div>
        <button class="circle-button" type="button">›</button>
      </div>

      <div class="chip-row">
        <button class="chip selected" type="button">Today</button>
        <button class="chip active-blue" type="button">Day</button>
        <button class="chip" type="button">Week</button>
        <button class="chip" type="button">Month</button>
      </div>

      <article class="saint-highlight glass-subtle">
        <div class="saint-date">
          <strong>13</strong>
          <span>Saint Martin I</span>
        </div>
        <div class="saint-photo"></div>
        <div class="saint-action">↗</div>
      </article>

      <button class="search-cta" type="button">Search Saints</button>

      <div class="season-row">
        <span>Advent</span>
        <span>Christmas</span>
        <span>Lent</span>
        <span>Easter</span>
        <span>Ordinary Time</span>
      </div>
    </section>
  `,
})
export class SaintsPageComponent {}
