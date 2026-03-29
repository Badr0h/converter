import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { UpgradePromptService } from '../services/upgrade-prompt.service';

/**
 * Limit Exceeded Interceptor
 * 
 * Detects 429 (Too Many Requests) with LIMIT_REACHED error
 * Automatically shows upgrade prompt and redirects to pricing
 * 
 * Features:
 * - Intercepts HTTP 429 errors
 * - Extracts upgrade information from response
 * - Shows upgrade prompt (via UpgradePromptService)
 * - Provides helpful upgrade suggestions
 */
@Injectable()
export class LimitExceededInterceptor implements HttpInterceptor {

  constructor(private upgradePromptService: UpgradePromptService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error) => {
        // Check if this is a limit exceeded error (429 Too Many Requests)
        if (error.status === 429 && error.error?.error === 'LIMIT_REACHED') {
          // Show upgrade prompt
          this.upgradePromptService.showUpgradePrompt(error.error);
          
          // You can optionally redirect to pricing page
          // window.location.href = error.error.upgradeUrl;
        }
        
        // Re-throw the error so it can be handled by the component
        return throwError(() => error);
      })
    );
  }
}
