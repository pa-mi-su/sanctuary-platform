import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'admin', loadComponent: () => import('./admin/admin-page.component').then(m => m.AdminPageComponent) },
  // Public navigation remains managed by the existing app shell.
  { path: '**', children: [] },
];
