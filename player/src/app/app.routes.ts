import { Routes } from '@angular/router';

import { signedIn, signedOut } from './core/guards';
import { l } from './i18n/i18n';

export const routes: Routes = [
  {
    path: 'login',
    title: () => l().titleSignIn,
    canActivate: [signedOut],
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      // The server owns / and /venues/:slug (whole pages for search engines, see the API's DirectoryController);
      // inside the app they lead to the app's own explore and venue screens
      { path: '', pathMatch: 'full', redirectTo: 'explore' },
      {
        path: 'explore',
        title: () => l().titleHome,
        loadComponent: () => import('./features/explore/explore').then((m) => m.Explore),
      },
      { path: 'venues/:slug', pathMatch: 'full', redirectTo: 'venues/:slug/slots' },
      {
        path: 'venues/:slug/slots',
        loadComponent: () => import('./features/venue/venue').then((m) => m.VenuePage),
      },
      {
        path: 'venues/:slug/book',
        title: () => l().titleBook,
        canActivate: [signedIn],
        loadComponent: () => import('./features/book/book').then((m) => m.Book),
      },
      {
        path: 'bookings',
        title: () => l().titleBookings,
        canActivate: [signedIn],
        loadComponent: () => import('./features/bookings/bookings').then((m) => m.Bookings),
      },
      {
        path: 'bookings/:id',
        title: () => l().titleBooking,
        canActivate: [signedIn],
        loadComponent: () => import('./features/bookings/booking').then((m) => m.BookingPage),
      },
      {
        path: 'deposit',
        title: () => l().titleDeposit,
        canActivate: [signedIn],
        loadComponent: () => import('./features/deposit/deposit').then((m) => m.DepositPage),
      },
      {
        path: 'messages',
        title: () => l().titleMessages,
        canActivate: [signedIn],
        loadComponent: () => import('./features/messages/messages').then((m) => m.Messages),
      },
      {
        path: 'notifications',
        title: () => l().titleNotifications,
        canActivate: [signedIn],
        loadComponent: () => import('./features/notifications/notifications').then((m) => m.Notifications),
      },
      {
        path: 'favourites',
        title: () => l().titleFavourites,
        canActivate: [signedIn],
        loadComponent: () => import('./features/favourites/favourites').then((m) => m.Favourites),
      },
      {
        path: 'reviews',
        title: () => l().titleReviews,
        canActivate: [signedIn],
        loadComponent: () => import('./features/reviews/reviews').then((m) => m.Reviews),
      },
      {
        path: 'waitlist',
        title: () => l().titleWaitlist,
        canActivate: [signedIn],
        loadComponent: () => import('./features/waitlist/waitlist').then((m) => m.Waitlist),
      },
      {
        path: 'bookings/:id/split',
        title: () => l().titleSplit,
        canActivate: [signedIn],
        loadComponent: () => import('./features/split/split').then((m) => m.SplitPage),
      },
      {
        path: 'splits',
        title: () => l().titleDues,
        canActivate: [signedIn],
        loadComponent: () => import('./features/split/dues').then((m) => m.Dues),
      },
      {
        path: 'memberships',
        title: () => l().titleMemberships,
        canActivate: [signedIn],
        loadComponent: () => import('./features/memberships/memberships').then((m) => m.Memberships),
      },
      {
        path: 'matches',
        title: () => l().titleGames,
        canActivate: [signedIn],
        loadComponent: () => import('./features/matches/matches').then((m) => m.Matches),
      },
      {
        path: 'matches/new',
        title: () => l().titleNewGame,
        canActivate: [signedIn],
        loadComponent: () => import('./features/matches/new-match').then((m) => m.NewMatch),
      },
      {
        path: 'matches/:id',
        title: () => l().titleGame,
        canActivate: [signedIn],
        loadComponent: () => import('./features/matches/match').then((m) => m.MatchPage),
      },
      {
        path: 'tournaments',
        title: () => l().titleTournaments,
        loadComponent: () => import('./features/tournaments/tournaments').then((m) => m.Tournaments),
      },
      {
        path: 'tournaments/:slug',
        title: () => l().titleTournament,
        loadComponent: () => import('./features/tournaments/tournament').then((m) => m.TournamentPage),
      },
      {
        path: 'teams',
        title: () => l().titleTeams,
        canActivate: [signedIn],
        loadComponent: () => import('./features/teams/teams').then((m) => m.Teams),
      },
      {
        path: 'teams/:slug',
        title: () => l().titleTeam,
        canActivate: [signedIn],
        loadComponent: () => import('./features/teams/team').then((m) => m.TeamPage),
      },
      {
        path: 'me/profile',
        title: () => l().titleProfile,
        canActivate: [signedIn],
        loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
      },
      {
        path: 'me',
        title: () => l().titleMe,
        canActivate: [signedIn],
        loadComponent: () => import('./features/me/me').then((m) => m.Me),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
