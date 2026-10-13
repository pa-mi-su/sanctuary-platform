import { Routes } from '@angular/router';

// The app shell renders this page; register the URL so initial router navigation preserves it.
export const routes: Routes = [
  { path: 'news', children: [] },
  { path: 'admin', children: [] },
  { path: 'admin/news', children: [] },
];
