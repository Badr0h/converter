import { ApplicationConfig, APP_INITIALIZER, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { LoggerService, initLogger } from './core/services/logger.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
    // ── Production log suppressor ──────────────────────────────────────────
    // Runs before any component is created. In production mode this replaces
    // console.log/warn/debug/info with no-ops so nothing leaks in DevTools.
    {
      provide: APP_INITIALIZER,
      useFactory: initLogger,
      deps: [LoggerService],
      multi: true
    }
  ]
};