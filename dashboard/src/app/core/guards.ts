import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Session } from './session';

export const signedIn: CanActivateFn = (_route, state) => {
  const session = inject(Session);
  return session.signedIn() ? true : inject(Router).createUrlTree(['/login'], { queryParams: { next: state.url } });
};

export const signedOut: CanActivateFn = () => {
  return inject(Session).signedIn() ? inject(Router).createUrlTree(['/']) : true;
};
