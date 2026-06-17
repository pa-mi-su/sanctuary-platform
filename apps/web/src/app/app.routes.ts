import { Routes } from '@angular/router';
import { AdminDashboardComponent } from './pages/admin/admin-dashboard.component';

export const routes: Routes = [
  { path: 'admin', component: AdminDashboardComponent },
  { path: 'admin/users', component: AdminDashboardComponent },
  { path: 'admin/notifications', component: AdminDashboardComponent },
];
