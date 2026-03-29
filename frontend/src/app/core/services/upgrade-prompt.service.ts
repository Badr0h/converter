import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * UpgradePrompt Service
 * 
 * Handles upgrade flow when user hits plan limits
 * Detects LIMIT_REACHED errors and shows upgrade prompt
 * 
 * Features:
 * - Centralized upgrade prompt handling
 * - Debounced to avoid spam
 * - Tracks last prompt time (show once per session)
 * - Redirect to pricing page
 */
@Injectable({
  providedIn: 'root'
})
export class UpgradePromptService {

  private upgradePrompt$ = new Subject<UpgradePromptData>();
  public upgradePromptData$ = this.upgradePrompt$.asObservable();

  private lastPromptTime = 0;
  private readonly PROMPT_DEBOUNCE_MS = 1000; // Prevent spam (show once per 1s)

  constructor() {}

  /**
   * Show upgrade prompt when limit reached
   */
  showUpgradePrompt(errorResponse: any): void {
    const now = Date.now();
    if (now - this.lastPromptTime < this.PROMPT_DEBOUNCE_MS) {
      return; // Debounce - don't show too frequently
    }
    this.lastPromptTime = now;

    const promptData: UpgradePromptData = {
      error: errorResponse.error || 'LIMIT_REACHED',
      message: errorResponse.message || 'Upgrade your plan',
      limitType: errorResponse.limitType || 'UNKNOWN',
      currentUsage: errorResponse.currentUsage || 0,
      limitValue: errorResponse.limitValue || 0,
      upgradePlanName: errorResponse.upgradePlanName || 'PRO',
      upgradeUrl: errorResponse.upgradeUrl || '/pricing',
      nextResetTime: errorResponse.nextResetTime,
      upgradePlan: errorResponse.upgradePlan
    };

    this.upgradePrompt$.next(promptData);
  }

  /**
   * Handled response with error status code
   */
  handleErrorResponse(error: any): boolean {
    // Check for 429 (Too Many Requests) - limit exceeded
    if (error.status === 429 && error.error?.error === 'LIMIT_REACHED') {
      this.showUpgradePrompt(error.error);
      return true;
    }
    return false;
  }
}

/**
 * Upgrade prompt data structure
 */
export interface UpgradePromptData {
  error: string; // "LIMIT_REACHED"
  message: string; // User-friendly message
  limitType: string; // "DAILY" or "MONTHLY"
  currentUsage: number; // Current usage count
  limitValue: number; // Plan limit
  upgradePlanName: string; // "PRO" or "ENTERPRISE"
  upgradeUrl: string; // "/pricing?plan=PRO"
  nextResetTime?: string; // ISO timestamp
  upgradePlan?: any; // Plan details
}
