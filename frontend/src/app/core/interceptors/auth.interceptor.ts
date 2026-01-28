import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // Get the auth token from the service
  const token = authService.getToken();

  // Debug logging
  console.log('Interceptor - Request URL:', req.url);
  console.log('Interceptor - Token exists:', !!token);
  console.log('Interceptor - Token:', token ? token.substring(0, 20) + '...' : 'null');

  // Clone the request and add authorization header if token exists
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    console.log('Interceptor - Added Authorization header');
  } else {
    console.warn('Interceptor - No token found, request will be unauthenticated');
  }

  // Handle the request and catch errors
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('Interceptor - HTTP Error:', error.status, error.statusText);
      if (error.status === 401 || error.status === 403) {
        // Unauthorized - logout and redirect to login
        console.log('Interceptor - Auth error, logging out');
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};