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
        path: 'payments',
        title: 'Payments · FutSlot',
        loadComponent: () => import('./features/payments/payments').then((m) => m.Payments),
      },
      ...ALL_ITEMS.filter((item) => !['schedule', 'bookings', 'payments', 'customers', 'reports', 'messages', 'venue/profile', 'venue/courts', 'venue/add-ons', 'venue/photos', 'venue/staff', 'venue/billing', 'grow/memberships', 'grow/reviews', 'grow/featured'].includes(item.path)).map((item) => ({
        path: item.path,
        title: `${item.label} · FutSlot`,
        data: { nav: item },
        loadComponent: () => import('./features/placeholder/placeholder').then((m) => m.Placeholder),
      })),
    ],
  },
  { path: '**', redirectTo: '' },
];
