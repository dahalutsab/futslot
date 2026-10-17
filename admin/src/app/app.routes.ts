import { Routes } from '@angular/router';

import { signedIn, signedOut } from './core/guards';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in · FutSlot admin',
    canActivate: [signedOut],
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [signedIn],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Overview · FutSlot admin',
        loadComponent: () => import('./features/overview/overview').then((m) => m.Overview),
      },
      {
        path: 'venues',
        title: 'Venues · FutSlot admin',
        loadComponent: () => import('./features/venues/venues').then((m) => m.Venues),
      },
      {
        path: 'venues/:id',
        title: 'Venue · FutSlot admin',
        loadComponent: () => import('./features/venues/venue').then((m) => m.VenuePage),
      },
      {
        path: 'payments',
        title: 'Payments to check · FutSlot admin',
        loadComponent: () => import('./features/payments/payments').then((m) => m.Payments),
      },
      {
        path: 'withdrawals',
        title: 'Deposits to send · FutSlot admin',
        loadComponent: () => import('./features/withdrawals/withdrawals').then((m) => m.Withdrawals),
      },
      {
        path: 'payouts',
        title: 'Venue payouts · FutSlot admin',
        loadComponent: () => import('./features/payouts/payouts').then((m) => m.Payouts),
      },
      {
        path: 'reviews',
        title: 'Reviews · FutSlot admin',
        loadComponent: () => import('./features/reviews/reviews').then((m) => m.Reviews),
      },
      {
        path: 'billing',
        title: 'Plans & billing · FutSlot admin',
        loadComponent: () => import('./features/billing/billing').then((m) => m.Billing),
      },
      {
        path: 'audit',
        title: 'Audit log · FutSlot admin',
        loadComponent: () => import('./features/audit/audit').then((m) => m.Audit),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
