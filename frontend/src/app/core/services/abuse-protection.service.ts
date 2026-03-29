import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Abuse Protection Service
 * 
 * Prevents user abuse by:
 * - Rate limiting rapid requests
 * - Detecting suspicious patterns
 * - Blocking users with too many failures
 * - Logging abuse attempts
 * 
 * Features:
 * - Request throttling (max 10 requests per minute)
 * - Error tracking (block after 5 failures)
 * - IP-based rate limiting (server-side)
 * - Captcha integration (optional)
 */
@Injectable({
  providedIn: 'root'
})
export class AbuseProtectionService {

  private requestTimestamps: number[] = [];
  private errorCount = 0;
  private lastErrorTime = 0;

  // Thresholds
  private readonly MAX_REQUESTS_PER_MINUTE = 10;
  private readonly MAX_ERRORS_ALLOWED = 5;
  private readonly ERROR_RESET_TIME_MS = 60000; // 1 minute
  private readonly BLOCK_USER_AFTER_MINUTES = 15;

  constructor(private router: Router) {}

  /**
   * Check if user is being abusive (too many requests)
   * @return true if user is rate-limited
   */
  isRateLimited(): boolean {
    const now = Date.now();
    const oneMinuteAgo = now - 60000;

    // Remove old timestamps (older than 1 minute)
    this.requestTimestamps = this.requestTimestamps.filter(ts => ts > oneMinuteAgo);

    // Check if over limit
    if (this.requestTimestamps.length >= this.MAX_REQUESTS_PER_MINUTE) {
      console.warn('[AbuseProtection] Rate limit exceeded: %d requests/min', 
        this.requestTimestamps.length);
      return true;
    }

    // Record this request
    this.requestTimestamps.push(now);
    return false;
  }

  /**
   * Record an error from user action
   * @return true if user should be blocked
   */
  recordError(): boolean {
    const now = Date.now();
    
    // Reset error count if time has passed
    if (now - this.lastErrorTime > this.ERROR_RESET_TIME_MS) {
      this.errorCount = 0;
    }

    this.errorCount++;
    this.lastErrorTime = now;

    if (this.errorCount >= this.MAX_ERRORS_ALLOWED) {
      console.error('[AbuseProtection] Maximum errors reached: %d | Blocking user', 
        this.errorCount);
      return true;
    }

    return false;
  }

  /**
   * Get remaining quota for user
   */
  getRemainingQuota(): number {
    const oneMinuteAgo = Date.now() - 60000;
    const recentRequests = this.requestTimestamps.filter(ts => ts > oneMinuteAgo).length;
    return Math.max(0, this.MAX_REQUESTS_PER_MINUTE - recentRequests);
  }

  /**
   * Show rate limit warning to user
   */
  showRateLimitWarning(): void {
    const remaining = Math.ceil((this.ERROR_RESET_TIME_MS - (Date.now() - this.lastErrorTime)) / 1000);
    alert(`⚠️ Too many requests. Please wait ${remaining}s and try again.\n\n` +
      `Limit: ${this.MAX_REQUESTS_PER_MINUTE} requests per minute.`);
  }

  /**
   * Block user and redirect
   */
  blockUser(reason: string): void {
    console.error('[AbuseProtection] Blocking user: %s', reason);
    localStorage.setItem('blocked_until', (Date.now() + this.BLOCK_USER_AFTER_MINUTES * 60000).toString());
    localStorage.setItem('block_reason', reason);
    
    // Redirect to blocked page
    this.router.navigate(['/blocked'], {
      queryParams: { reason },
      replaceUrl: true
    });
  }

  /**
   * Check if user is currently blocked
   */
  isBlocked(): boolean {
    const blockedUntil = localStorage.getItem('blocked_until');
    if (!blockedUntil) return false;

    const unblockTime = parseInt(blockedUntil, 10);
    const isStillBlocked = Date.now() < unblockTime;

    if (!isStillBlocked) {
      localStorage.removeItem('blocked_until');
      localStorage.removeItem('block_reason');
    }

    return isStillBlocked;
  }

  /**
   * Reset protection (for testing)
   */
  reset(): void {
    this.requestTimestamps = [];
    this.errorCount = 0;
    this.lastErrorTime = 0;
  }
}
