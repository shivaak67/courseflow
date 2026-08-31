import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getToken();

  const isPublicAuth =
    req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register');

  const authReq =
    token && !isPublicAuth
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((err) => {
      if (err.status === 401 && !isPublicAuth) {
        auth.logout();
        void router.navigateByUrl('/auth/login');
      }
      return throwError(() => err);
    }),
  );
};
