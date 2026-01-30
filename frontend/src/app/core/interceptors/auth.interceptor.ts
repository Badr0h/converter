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
  console.log('=== AUTH INTERCEPTOR DEBUG ===');
  console.log('Request URL:', req.url);
  console.log('Request Method:', req.method);
  console.log('Token exists:', !!token);
  console.log('Token length:', token ? token.length : 0);
  console.log('Token preview:', token ? token.substring(0, 50) + '...' : 'null');
  console.log('Is user authenticated:', authService.isAuthenticated());
  console.log('Current user:', authService.currentUserValue);
  console.log('==============================');

  // Clone the request and add authorization header if token exists
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    console.log('✅ Added Authorization header');
  } else {
    console.warn('❌ No token found, request will be unauthenticated');
  }

  // Handle the request and catch errors
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('=== HTTP ERROR ===');
      console.error('Status:', error.status);
      console.error('Status Text:', error.statusText);
      console.error('URL:', error.url);
      console.error('Error message:', error.message);
      
      if (error.status === 401 || error.status === 403) {
        console.log('🚫 Auth error detected - logging out');
        console.log('User was logged out due to:', error.status === 401 ? 'Unauthorized' : 'Forbidden');
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};