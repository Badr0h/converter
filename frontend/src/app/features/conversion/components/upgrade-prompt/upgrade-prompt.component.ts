import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { UpgradePromptService, UpgradePromptData } from '../../../../core/services/upgrade-prompt.service';

/**
 * Upgrade Prompt Modal Component
 * 
 * Shows when user reaches plan limit
 * Displays:
 * - Current usage vs limit
 * - Suggested upgrade plan
 * - "Upgrade Now" button
 */
@Component({
  selector: 'app-upgrade-prompt',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="upgrade-modal-overlay" *ngIf="promptData" (click)="closePrompt()">
      <div class="upgrade-modal" (click)="$event.stopPropagation()">
        <!-- Header -->
        <div class="upgrade-header">
          <h2>⚠️ Plan Limit Reached</h2>
          <button class="close-btn" (click)="closePrompt()">✕</button>
        </div>

        <!-- Content -->
        <div class="upgrade-body">
          <p class="upgrade-message">{{ promptData.message }}</p>

          <!-- Limit Details -->
          <div class="limit-details">
            <div class="stat">
              <span class="label">{{ promptData.limitType }} Limit:</span>
              <span class="value">{{ promptData.currentUsage }} / {{ promptData.limitValue }}</span>
            </div>
            
            <div class="progress-bar">
              <div class="progress-fill" style="width: 100%"></div>
            </div>

            <p class="reset-info">
              <span *ngIf="promptData.limitType === 'DAILY'">
                ⏰ Reset tomorrow at midnight
              </span>
              <span *ngIf="promptData.limitType === 'MONTHLY'">
                ⏰ Reset on {{ getNextMonthDate() }}
              </span>
            </p>
          </div>

          <!-- Upgrade Suggestion -->
          <div class="upgrade-suggestion">
            <p class="suggestion-text">
              Upgrade to <strong>{{ promptData.upgradePlanName }}</strong> for:
            </p>
            <ul class="benefits" *ngIf="promptData.upgradePlan">
              <li>✓ {{ promptData.upgradePlan.monthlyLimit }} conversions/month</li>
              <li>✓ Priority support</li>
              <li>✓ Advanced AI models</li>
            </ul>
          </div>
        </div>

        <!-- Actions -->
        <div class="upgrade-footer">
          <button class="btn-secondary" (click)="closePrompt()">
            Not Now
          </button>
          <button class="btn-primary" (click)="upgradeNow()">
            💳 Upgrade Now
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .upgrade-modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .upgrade-modal {
      background: white;
      border-radius: 12px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
      max-width: 500px;
      width: 90%;
      animation: slideUp 0.3s ease;
    }

    @keyframes slideUp {
      from { transform: translateY(20px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }

    .upgrade-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px;
      border-bottom: 1px solid #e5e7eb;
      
      h2 {
        margin: 0;
        font-size: 20px;
        color: #dc2626;
      }

      .close-btn {
        background: none;
        border: none;
        font-size: 24px;
        color: #6b7280;
        cursor: pointer;
        padding: 0;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        transition: background 0.2s;

        &:hover {
          background: #f3f4f6;
        }
      }
    }

    .upgrade-body {
      padding: 24px;
    }

    .upgrade-message {
      margin: 0 0 20px 0;
      font-size: 16px;
      color: #374151;
      line-height: 1.5;
    }

    .limit-details {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 20px;

      .stat {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;
        font-weight: 500;

        .label { color: #6b7280; }
        .value { color: #dc2626; }
      }

      .progress-bar {
        width: 100%;
        height: 8px;
        background: #fed7d7;
        border-radius: 4px;
        overflow: hidden;
        margin-bottom: 12px;

        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #ef4444, #dc2626);
        }
      }

      .reset-info {
        margin: 0;
        font-size: 13px;
        color: #7f1d1d;
        font-weight: 500;
      }
    }

    .upgrade-suggestion {
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: 8px;
      padding: 16px;

      .suggestion-text {
        margin: 0 0 12px 0;
        font-size: 14px;
        color: #374151;
      }

      .benefits {
        margin: 0;
        padding-left: 20px;
        color: #059669;
        font-size: 14px;
        font-weight: 500;

        li {
          margin: 6px 0;
        }
      }
    }

    .upgrade-footer {
      padding: 20px;
      border-top: 1px solid #e5e7eb;
      display: flex;
      gap: 12px;
      justify-content: flex-end;

      button {
        padding: 10px 20px;
        border-radius: 6px;
        font-weight: 600;
        border: none;
        cursor: pointer;
        transition: all 0.2s;
      }

      .btn-secondary {
        background: #f3f4f6;
        color: #374151;

        &:hover {
          background: #e5e7eb;
        }
      }

      .btn-primary {
        background: #3b82f6;
        color: white;

        &:hover {
          background: #2563eb;
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
        }
      }
    }
  `]
})
export class UpgradePromptComponent implements OnInit, OnDestroy {

  promptData: UpgradePromptData | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private upgradePromptService: UpgradePromptService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.upgradePromptService.upgradePromptData$
      .pipe(takeUntil(this.destroy$))
      .subscribe((promptData: UpgradePromptData) => {
        this.promptData = promptData;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  closePrompt(): void {
    this.promptData = null;
  }

  upgradeNow(): void {
    if (this.promptData?.upgradeUrl) {
      // Either navigate internally or open external link
      if (this.promptData.upgradeUrl.startsWith('/')) {
        this.router.navigate([this.promptData.upgradeUrl]);
      } else {
        window.location.href = this.promptData.upgradeUrl;
      }
    }
    this.closePrompt();
  }

  getNextMonthDate(): string {
    const nextMonth = new Date();
    nextMonth.setMonth(nextMonth.getMonth() + 1);
    nextMonth.setDate(1);
    return nextMonth.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }
}
