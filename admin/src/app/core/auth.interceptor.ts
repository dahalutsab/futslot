import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { Session } from './session';

const PUBLIC = ['/api/v1/auth/'];

/**
 * Asks the API for English, and adds the access token to API calls, and on a 401 refreshes it once and retries.
 * This app is in English, so its errors and the codes it texts should be too, whatever the browser's language.
 */
export const authInterceptor: HttpInterceptorFn = (original, next) => {
  if (!original.url.startsWith('/api/')) {
    return next(original);
  }
  const request = original.clone({ setHeaders: { 'Accept-Language': 'en' } });
  if (PUBLIC.some((path) => request.url.startsWith(path))) {
    return next(request);
  }
  const session = inject(Session);
  const withToken = (req: HttpRequest<unknown>) => {
    const token = session.accessToken();
    return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  };
  return next(withToken(request)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !session.signedIn()) {
        return throwError(() => error);
      }
      return session.refresh().pipe(
        catchError((refreshError: unknown) => {
          session.expired();
          return throwError(() => refreshError);
        }),
        switchMap(() => next(withToken(request))),
      );
    }),
  );
};
