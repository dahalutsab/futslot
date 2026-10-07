import { HttpErrorResponse } from '@angular/common/http';

import { ApiProblem } from './api.types';

/** The API's error code and a sentence to show, for any failed request. */
export function problemOf(error: unknown): { code: string | null; message: string } {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return { code: 'OFFLINE', message: "Can't reach FutSlot. Check your connection and try again." };
    }
    const body = error.error as Partial<ApiProblem> | null;
    if (body && typeof body === 'object' && body.detail) {
      return { code: body.code ?? null, message: body.detail };
    }
  }
  return { code: null, message: 'Something went wrong. Please try again.' };
}
