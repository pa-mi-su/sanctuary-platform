import { TestBed } from '@angular/core/testing';

import { NovenaDetail, SaintDetail } from '../core/api/sanctuary-api.service';
import { ContentDetailModalComponent } from './content-detail-modal.component';

describe('ContentDetailModalComponent', () => {
  const saint = {
    id: 'saint_francis_of_assisi',
    slug: 'saint-francis-of-assisi',
    name: 'Saint Francis of Assisi',
    feastMonth: 10,
    feastDay: 4,
    feastLabel: 'October 4',
    imageUrl: null,
    patronages: [],
    summary: 'A saint.',
    biography: 'A biography.',
  } as unknown as SaintDetail;
  const novena = {
    id: 'novena_our_lady_of_lourdes',
    slug: 'our-lady-of-lourdes-novena',
    title: 'Our Lady of Lourdes Novena',
    description: 'A novena.',
    imageUrl: null,
    intentions: [],
    days: [],
  } as unknown as NovenaDetail;

  it('shows the favorite action and requests an account when signed out', () => {
    const fixture = TestBed.createComponent(ContentDetailModalComponent);
    fixture.componentRef.setInput('saintDetail', saint);
    fixture.componentRef.setInput('isAuthenticated', false);
    const accountRequired = vi.spyOn(fixture.componentInstance.accountRequired, 'emit');
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('.favorite-button') as HTMLButtonElement;
    expect(button).toBeTruthy();
    button.click();

    expect(accountRequired).toHaveBeenCalledWith('favorite-saint');
  });

  it('emits the favorite mutation when signed in', () => {
    const fixture = TestBed.createComponent(ContentDetailModalComponent);
    fixture.componentRef.setInput('saintDetail', saint);
    fixture.componentRef.setInput('isAuthenticated', true);
    const toggleFavorite = vi.spyOn(fixture.componentInstance.toggleSaintFavorite, 'emit');
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.favorite-button') as HTMLButtonElement).click();

    expect(toggleFavorite).toHaveBeenCalledOnce();
  });

  it('shows Start Novena and requests an account when signed out', () => {
    const fixture = TestBed.createComponent(ContentDetailModalComponent);
    fixture.componentRef.setInput('novenaDetail', novena);
    fixture.componentRef.setInput('isAuthenticated', false);
    const accountRequired = vi.spyOn(fixture.componentInstance.accountRequired, 'emit');
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('.primary-action') as HTMLButtonElement;
    expect(button.textContent).toContain('Start Novena');
    button.click();

    expect(accountRequired).toHaveBeenCalledWith('start-novena');
  });
});
