import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Auth HTTP Interceptor
 * - Attaches Bearer token to every outgoing request.
 * - Handles 401/403 by logging the user out.
 * - In production (environment.production === true) all debug console.log calls
 *   are suppressed so sensitive session data does not appear in DevTools Console.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();
  const isProd = environment.production;

  // ── Debug logging – DEV ONLY ─────────────────────────────────────────────
  if (!isProd) {
    console.log('=== AUTH INTERCEPTOR DEBUG ===');
    console.log('Request URL:', req.url);
    console.log('Request Method:', req.method);
    console.log('Token exists:', !!token);
    console.log('Token length:', token ? token.length : 0);
    console.log('Is user authenticated:', authService.isAuthenticated());
    console.log('==============================');
  }
  // ─────────────────────────────────────────────────────────────────────────

  // Attach Authorization header if a token is present
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    if (!isProd) {
      console.log('✅ Added Authorization header');
    }
  } else {
    if (!isProd) {
      console.warn('❌ No token found, request will be unauthenticated');
    }
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // In production only log the status/url – never the full error object
      // which may contain token details or internal server messages.
      if (!isProd) {
        console.error('=== HTTP ERROR ===');
        console.error('Status:', error.status);
        console.error('Status Text:', error.statusText);
        console.error('URL:', error.url);
        console.error('Error message:', error.message);
      }

      if (error.status === 401 || error.status === 403) {
        if (!isProd) {
          console.log('🚫 Auth error detected – logging out. Code:', error.status);
        }
        authService.logout();
      }

      return throwError(() => error);
    })
  );
};