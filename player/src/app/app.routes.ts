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
    path: '',
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'FutSlot · Book futsal in Nepal',
        loadComponent: () => import('./features/explore/explore').then((m) => m.Explore),
      },
      {
        path: 'venues/:slug',
        loadComponent: () => import('./features/venue/venue').then((m) => m.VenuePage),
      },
      {
        path: 'venues/:slug/book',
        title: 'Book · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/book/book').then((m) => m.Book),
      },
      {
        path: 'bookings',
        title: 'Your bookings · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/bookings/bookings').then((m) => m.Bookings),
      },
      {
        path: 'bookings/:id',
        title: 'Booking · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/bookings/booking').then((m) => m.BookingPage),
      },
      {
        path: 'deposit',
        title: 'Booking deposit · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/deposit/deposit').then((m) => m.DepositPage),
      },
      {
        path: 'me',
        title: 'Me · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/me/me').then((m) => m.Me),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
