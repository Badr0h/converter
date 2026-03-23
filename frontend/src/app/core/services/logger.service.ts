import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * LoggerService – Production-safe console wrapper.
 *
 * Calling `suppressLogsInProduction()` once at app startup
 * replaces `console.log`, `console.warn`, and `console.debug`
 * with no-ops when `environment.production === true`.
 *
 * `console.error` is intentionally kept alive so that
 * genuine runtime errors still surface in error monitoring tools
 * (e.g., Sentry, Datadog).
 *
 * Usage (one-time, in app.config.ts via APP_INITIALIZER):
 *   { provide: APP_INITIALIZER, useFactory: initLogger, deps: [LoggerService], multi: true }
 */
@Injectable({ providedIn: 'root' })
export class LoggerService {

  suppressLogsInProduction(): void {
    if (environment.production) {
      // Replace verbose methods with no-ops
      console.log   = () => {};
      console.warn  = () => {};
      console.debug = () => {};
      console.info  = () => {};
      // console.error is kept intact for monitoring tools
    }
  }

  // ── Convenience wrappers (use these in components instead of console.*) ──

  log(...args: any[]): void {
    if (!environment.production) {
      console.log(...args);
    }
  }

  warn(...args: any[]): void {
    if (!environment.production) {
      console.warn(...args);
    }
  }

  debug(...args: any[]): void {
    if (!environment.production) {
      console.debug(...args);
    }
  }

  /** Always logs – use for genuine errors you want monitored in production */
  error(...args: any[]): void {
    console.error(...args);
  }
}

/** Factory for APP_INITIALIZER */
export function initLogger(logger: LoggerService): () => void {
  return () => logger.suppressLogsInProduction();
}
