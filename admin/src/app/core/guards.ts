import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Session } from './session';

/** Signed in as a FutSlot admin. */
export const signedIn: CanActivateFn = (_route, state) => {
  const session = inject(Session);
  return session.admin() ? true : inject(Router).createUrlTree(['/login'], { queryParams: { next: state.url } });
};

export const signedOut: CanActivateFn = () => {
  return inject(Session).admin() ? inject(Router).createUrlTree(['/']) : true;
};
