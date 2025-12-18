import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { SubscriptionResponseDto } from '../../../core/models/subscription.model';
import { PlanResponseDto } from '../../../core/models/plan.model';
import { PaymentService } from '../../../core/services/payment.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-plans',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './plans.component.html',
  styleUrls: ['./plans.component.scss']
})
export class PlansComponent implements OnInit {
  plans: PlanResponseDto[] = [];
  currentSubscription: SubscriptionResponseDto | null = null;
  loading = true;
  errorMessage = '';
  billingCycle: 'monthly' | 'annual' = 'monthly';

  constructor(
    private subscriptionService: SubscriptionService,
    private router: Router
    , private paymentService: PaymentService
    , private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadPlans();
    this.loadCurrentSubscription();
  }

  loadPlans(): void {
    this.subscriptionService.getPlans().subscribe({
      next: (plans) => {
        // Hide FREE plan for now
        const visible = plans.filter(p => p.name?.toUpperCase() !== 'FREE');
        this.plans = visible.sort((a, b) => ((a.monthlyPrice ?? a.price) - (b.monthlyPrice ?? b.price)));
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading plans', error);
        this.errorMessage = 'Failed to load subscription plans';
        this.loading = false;
      }
    });
  }

  loadCurrentSubscription(): void {
    this.subscriptionService.getCurrentSubscription().subscribe({
      next: (subscription) => {
        this.currentSubscription = subscription[0] || null;
        if (this.currentSubscription) {
          console.log('Current subscription:', this.currentSubscription);
        }
      },
      error: (error) => {
        console.error('Error loading current subscription', error);
        // User might not have a subscription yet
        this.currentSubscription = null;
      }
    });
  }



  selectPlan(plan: PlanResponseDto): void {
    // Check if user is already on this plan
    if (this.currentSubscription?.planName === plan.name) {
      console.log('Already subscribed to', plan.name);
      return;
    }

    // Redirect to checkout for payment — subscription will be created as PENDING there
    this.router.navigate(['/subscription/checkout', plan.id], { queryParams: { billing: this.billingCycle } });
  }

  isCurrentPlan(plan: PlanResponseDto): boolean {
    if (!this.currentSubscription) return false;
    // Check both planName and plan id
    return this.currentSubscription.planName === plan.name || this.currentSubscription.planId === plan.id;
  }

  getCurrentPlanName(): string {
    return this.currentSubscription?.planName || 'None';
  }

  getPlanButtonText(plan: PlanResponseDto): string {
    if (this.isCurrentPlan(plan)) {
      return 'Current Plan';
    }
    
    if (plan.price === 0) {
      return 'Free Plan';
    }

    if (this.currentSubscription) {
      // Get monthly price for comparison
      const currentPrice = this.currentSubscription.monthlyPrice ?? this.currentSubscription.price ?? 0;
      const planPrice = plan.monthlyPrice ?? plan.price ?? 0;
      
      if (currentPrice < planPrice) {
        return 'Upgrade';
      } else if (currentPrice > planPrice) {
        return 'Downgrade';
      } else {
        return 'Switch Plan';
      }
    }

    return 'Get Started';
  }

  getPlanBadge(plan: PlanResponseDto): string | null {
    if (plan.name === 'Pro') {
      return 'Popular';
    }
    if (plan.name === 'Business') {
      return 'Best Value';
    }
    return null;
  }

  toggleBilling(cycle: 'monthly' | 'annual'){
    this.billingCycle = cycle;
  }

  private mapPlanDurationToEnum(durationValue: number | string) {
    const n = Number(durationValue);
    if (isNaN(n)) return 'ONE_MONTH';
    if (n === 1 || n === 30) return 'ONE_MONTH';
    if (n === 3 || n === 90) return 'THREE_MONTHS';
    if (n === 12 || n === 365) return 'TWELVE_MONTHS';
    if (n >= 360) return 'TWELVE_MONTHS';
    if (n >= 80) return 'THREE_MONTHS';
    return 'ONE_MONTH';
  }

  private localDateString(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  formatPrice(price: number, currency: string): string {
    if (price === 0) {
      return 'Free';
    }
    // Use currency if provided
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(price);
    } catch {
      return `$${price.toFixed(2)}`;
    }
  }

  formatConversions(maxConversions?: number): string {
    if (maxConversions === undefined || maxConversions === null) {
      return '—';
    }
    if (maxConversions === -1) {
      return 'Unlimited';
    }
    return maxConversions.toString();
  }
}