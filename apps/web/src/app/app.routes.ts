import { Routes } from '@angular/router';
import { adminGuard } from './pages/admin/admin.guard';

export const routes: Routes = [
  {
    path: 'sanctuary-ops',
    loadComponent: () => import('./pages/admin/admin-dashboard.component').then((module) => module.AdminDashboardComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'sanctuary-ops/users',
    loadComponent: () => import('./pages/admin/admin-dashboard.component').then((module) => module.AdminDashboardComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'sanctuary-ops/notifications',
    loadComponent: () => import('./pages/admin/admin-dashboard.component').then((module) => module.AdminDashboardComponent),
    canActivate: [adminGuard],
  },
  { path: 'admin', redirectTo: '/', pathMatch: 'full' },
  { path: 'admin/**', redirectTo: '/' },
];
