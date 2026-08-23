import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/**
 * Stub guard: checks the fake token flag set by AuthService.mockLogin.
 * Allows all when no token is required for shell browsing — currently
 * redirects to login only if `requireAuth` is true on the route data.
 * Agent A will replace this with real JWT validation.
 */
export const authGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requireAuth = route.data['requireAuth'] === true;

  if (!requireAuth || auth.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/auth/login']);
};
