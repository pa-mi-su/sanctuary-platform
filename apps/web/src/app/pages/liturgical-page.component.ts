import { Component } from '@angular/core';

@Component({
  selector: 'app-liturgical-page',
  standalone: true,
  template: `
    <section class="screen-card glass-card">
      <div class="screen-header split">
        <button class="circle-button" type="button">‹</button>
        <div class="screen-title">
          <h1>April 2026</h1>
          <p>Liturgical • Tap to jump</p>
        </div>
        <button class="circle-button" type="button">›</button>
      </div>

      <div class="chip-row">
        <button class="chip selected" type="button">Today</button>
        <button class="chip" type="button">Day</button>
        <button class="chip" type="button">Week</button>
        <button class="chip active-blue" type="button">Month</button>
      </div>

      <div class="calendar-headings">
        <span>Sun</span>
        <span>Mon</span>
        <span>Tue</span>
        <span>Wed</span>
        <span>Thu</span>
        <span>Fri</span>
        <span>Sat</span>
      </div>

      <div class="calendar-grid">
        <div class="calendar-day accent-purple"><strong>1</strong><span>Wednesday...</span></div>
        <div class="calendar-day accent-purple"><strong>2</strong><span>Holy Thur...</span></div>
        <div class="calendar-day accent-purple"><strong>3</strong><span>Good Fri...</span></div>
        <div class="calendar-day accent-purple"><strong>4</strong><span>Holy Satur...</span></div>
        <div class="calendar-day"><strong>5</strong><span>Easter Sun...</span></div>
        <div class="calendar-day"><strong>6</strong><span>Easter Octa...</span></div>
        <div class="calendar-day"><strong>7</strong><span>Easter Octa...</span></div>
        <div class="calendar-day"><strong>8</strong><span>Easter Octa...</span></div>
        <div class="calendar-day"><strong>9</strong><span>Easter Octa...</span></div>
        <div class="calendar-day"><strong>10</strong><span>Easter Octa...</span></div>
        <div class="calendar-day"><strong>11</strong><span>Easter Octa...</span></div>
        <div class="calendar-day"><strong>12</strong><span>Second Su...</span></div>
        <div class="calendar-day active-gold"><strong>13</strong><span>Monday of...</span></div>
        <div class="calendar-day"><strong>14</strong><span>Tuesday of...</span></div>
        <div class="calendar-day"><strong>15</strong><span>Wednesday...</span></div>
      </div>

      <button class="search-cta" type="button">Search</button>

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
export class LiturgicalPageComponent {}
