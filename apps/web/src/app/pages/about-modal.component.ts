import { Component, output } from '@angular/core';

@Component({
  selector: 'app-about-modal',
  standalone: true,
  styleUrl: './about-modal.component.scss',
  template: `
    <div class="modal-backdrop" (click)="close.emit()"></div>
    <section class="about-modal glass-card" aria-label="About Sanctuary">
      <div class="about-header">
        <h2>About Sanctuary</h2>
        <button class="close-button" type="button" (click)="close.emit()">×</button>
      </div>

      <p class="about-copy">
        Sanctuary is a Catholic prayer companion focused on novenas, saints, prayers, and liturgical living.
      </p>

      <div class="about-links">
        <a href="https://mydailysanctuary.com/support/index.html" target="_blank" rel="noreferrer">Support</a>
        <a href="https://mydailysanctuary.com/privacy/index.html" target="_blank" rel="noreferrer">Privacy Policy</a>
      </div>
    </section>
  `,
})
export class AboutModalComponent {
  readonly close = output<void>();
}
