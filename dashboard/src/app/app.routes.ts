import { Routes } from '@angular/router';

import { signedIn, signedOut } from './core/guards';

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
      {
        path: 'schedule',
        title: 'Schedule · FutSlot',
        loadComponent: () => import('./features/schedule/schedule').then((m) => m.Schedule),
      },
      {
        path: 'bookings',
        title: 'Bookings · FutSlot',
        loadComponent: () => import('./features/bookings/bookings').then((m) => m.Bookings),
      },
      {
        path: 'customers',
        title: 'Customers · FutSlot',
        loadComponent: () => import('./features/customers/customers').then((m) => m.Customers),
      },
      {
        path: 'reports',
        title: 'Reports · FutSlot',
        loadComponent: () => import('./features/reports/reports').then((m) => m.Reports),
      },
      {
        path: 'messages',
        title: 'Messages · FutSlot',
        loadComponent: () => import('./features/messages/messages').then((m) => m.Messages),
      },
      {
        path: 'account',
        title: 'Your account · FutSlot',
        loadComponent: () => import('./features/account/account').then((m) => m.Account),
      },
      {
        path: 'venue/profile',
        title: 'Profile & hours · FutSlot',
        loadComponent: () => import('./features/venue/venue-profile').then((m) => m.VenueProfile),
      },
      {
        path: 'venue/courts',
        title: 'Courts & pricing · FutSlot',
        loadComponent: () => import('./features/venue/courts').then((m) => m.Courts),
      },
      {
        path: 'venue/add-ons',
        title: 'Extras · FutSlot',
        loadComponent: () => import('./features/venue/extras').then((m) => m.Extras),
      },
      {
        path: 'venue/photos',
        title: 'Photos · FutSlot',
        loadComponent: () => import('./features/venue/photos').then((m) => m.Photos),
      },
      {
        path: 'venue/staff',
        title: 'Staff · FutSlot',
        loadComponent: () => import('./features/venue/staff').then((m) => m.Staff),
      },
      {
        path: 'venue/billing',
        title: 'Plan & billing · FutSlot',
        loadComponent: () => import('./features/venue/billing').then((m) => m.Billing),
      },
      {
        path: 'grow/memberships',
        title: 'Memberships & codes · FutSlot',
        loadComponent: () => import('./features/grow/memberships').then((m) => m.Memberships),
      },
      {
        path: 'grow/reviews',
        title: 'Reviews · FutSlot',
        loadComponent: () => import('./features/grow/reviews').then((m) => m.Reviews),
      },
      {
        path: 'grow/featured',
        title: 'Featured listing · FutSlot',
        loadComponent: () => import('./features/grow/featured').then((m) => m.Featured),
      },
      {
        path: 'grow/tournaments',
        title: 'Tournaments · FutSlot',
        loadComponent: () => import('./features/grow/tournaments/tournaments').then((m) => m.Tournaments),
      },
      {
        path: 'grow/tournaments/:id',
        title: 'Tournament · FutSlot',
        loadComponent: () => import('./features/grow/tournaments/tournament').then((m) => m.Tournament),
      },
      {
        path: 'venue/website',
        title: 'Website · FutSlot',
        loadComponent: () => import('./features/venue/website').then((m) => m.Website),
      },
      {
        path: 'payments',
        title: 'Payments · FutSlot',
        loadComponent: () => import('./features/payments/payments').then((m) => m.Payments),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
