import { Routes } from '@angular/router';

import { signedIn, signedOut } from './core/guards';
import { ALL_ITEMS } from './layout/navigation';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in · FutSlot',
    canActivate: [signedOut],
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: 'no-venue',
    title: 'No venue · FutSlot',
    canActivate: [signedIn],
    loadComponent: () => import('./features/no-venue/no-venue').then((m) => m.NoVenue),
  },
  {
    path: '',
    canActivate: [signedIn],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'schedule' },
      ...ALL_ITEMS.map((item) => ({
        path: item.path,
        title: `${item.label} · FutSlot`,
        data: { nav: item },
        loadComponent: () => import('./features/placeholder/placeholder').then((m) => m.Placeholder),
      })),
    ],
  },
  { path: '**', redirectTo: '' },
];
