import { TestBed } from '@angular/core/testing';
import { AppFooterComponent } from './app-footer.component';

describe('AppFooterComponent', () => {
  it('renders the social and store destinations', async () => {
    await TestBed.configureTestingModule({ imports: [AppFooterComponent] }).compileComponents();
    const fixture = TestBed.createComponent(AppFooterComponent);

    fixture.detectChanges();

    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    expect(links.map((link) => link.href)).toEqual([
      'https://www.facebook.com/mydailysanctuary',
      'https://www.instagram.com/mydailysanctuary/',
      'https://apps.apple.com/us/app/sanctuary-catholic-companion/id6759986068?uo=4',
    ]);
    expect(fixture.nativeElement.textContent).toContain('Coming soon');
  });

  it('localizes the footer copy', async () => {
    await TestBed.configureTestingModule({ imports: [AppFooterComponent] }).compileComponents();
    const fixture = TestBed.createComponent(AppFooterComponent);
    fixture.componentRef.setInput('currentLanguage', 'pl');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Obserwuj nas');
    expect(fixture.nativeElement.textContent).toContain('Wkrótce');
  });
});
