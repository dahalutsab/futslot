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
        path: 'messages',
        title: 'Messages · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/messages/messages').then((m) => m.Messages),
      },
      {
        path: 'notifications',
        title: 'Notifications · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/notifications/notifications').then((m) => m.Notifications),
      },
      {
        path: 'favourites',
        title: 'Saved venues · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/favourites/favourites').then((m) => m.Favourites),
      },
      {
        path: 'reviews',
        title: 'Reviews · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/reviews/reviews').then((m) => m.Reviews),
      },
      {
        path: 'waitlist',
        title: 'Waitlist · FutSlot',
        canActivate: [signedIn],
        loadComponent: () => import('./features/waitlist/waitlist').then((m) => m.Waitlist),
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
