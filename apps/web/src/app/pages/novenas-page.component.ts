import { Component } from '@angular/core';

@Component({
  selector: 'app-novenas-page',
  standalone: true,
  template: `
    <section class="screen-card glass-card">
      <div class="screen-header">
        <button class="circle-button" type="button">‹</button>
        <div>
          <h1>Search Novenas</h1>
        </div>
      </div>

      <div class="search-bar glass-subtle">
        <span>⌕</span>
        <span>Search novenas</span>
      </div>

      <p class="meta-text">237 results</p>

      <div class="list-stack">
        <article class="content-card glass-subtle">
          <div class="content-card__icon">📘</div>
          <div class="content-card__body">
            <h2>30 Day Novena to St Joseph</h2>
            <p>Seek St. Joseph's powerful intercession and receive the protection of Jesus' foster father.</p>
            <span class="content-tag">30-day novena</span>
          </div>
          <span class="nav-arrow">›</span>
        </article>

        <article class="content-card glass-subtle">
          <div class="content-card__icon">📘</div>
          <div class="content-card__body">
            <h2>All Saints Novena</h2>
            <p>Unite with Heaven's greatest heroes today and invoke the intercession of countless saints.</p>
            <span class="content-tag">9-day novena</span>
          </div>
          <span class="nav-arrow">›</span>
        </article>

        <article class="content-card glass-subtle">
          <div class="content-card__icon">📘</div>
          <div class="content-card__body">
            <h2>Angelic Warfare Confraternity Novena</h2>
            <p>Conquer temptation and reclaim purity through the angelic protection promised by St. Thomas Aquinas.</p>
            <span class="content-tag">9-day novena</span>
          </div>
          <span class="nav-arrow">›</span>
        </article>
      </div>
    </section>
  `,
})
export class NovenasPageComponent {}
