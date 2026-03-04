import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd, ActivatedRoute } from '@angular/router';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { SubscriptionResponseDto, SubscriptionStatus } from '../../../core/models/subscription.model';
import { PlanResponseDto } from '../../../core/models/plan.model';
import { PaymentService } from '../../../core/services/payment.service';
import { AuthService } from '../../../core/services/auth.service';
import { filter } from 'rxjs/operators';
import { PLANS_DESCRIPTION } from './plan.constants';

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
  subscriptionStatus: SubscriptionStatus | null = null;
  loading = true;
  errorMessage = '';
  billingCycle: 'monthly' | 'annual' = 'monthly';
  readonly SubscriptionStatus = SubscriptionStatus;

  selectedPlanDetails: any = null;
  showPlanDetails = false;

  constructor(
    private subscriptionService: SubscriptionService,
    private router: Router,
    private route: ActivatedRoute,
    private paymentService: PaymentService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.loadPlans();
    this.loadCurrentSubscription();

    // Reload subscription data when returning to this page
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd && event.urlAfterRedirects.includes('/subscription/plans')))
      .subscribe(() => {
        console.log('Navigated back to plans page, reloading subscription data');
        this.loadCurrentSubscription();
      });

    // Also check for refresh parameter in route
    this.route.queryParams.subscribe(params => {
      if (params['refresh']) {
        console.log('Refresh parameter detected, reloading subscription data');
        this.loadCurrentSubscription();
      }
    });
  }

  loadPlans(): void {
    this.subscriptionService.getPlans().subscribe({
      next: (plans) => {
        this.plans = plans.map(p => {
          // Map backend ALL_CAPS name to match PLANS_DESCRIPTION keys (e.g., STARTER -> Starter)
          const planKey = p.name.charAt(0).toUpperCase() + p.name.slice(1).toLowerCase();
          const localDetails = PLANS_DESCRIPTION[planKey];
          return {
            ...p,
            features: p.features && p.features.length > 0 ? p.features : (localDetails?.features || [])
          };
        }).sort((a, b) => ((a.monthlyPrice ?? a.price) - (b.monthlyPrice ?? b.price)));
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
    this.subscriptionService.getCurrentUserSubscription().subscribe({
      next: (subscription) => {
        this.currentSubscription = subscription;
        if (this.currentSubscription) {
          this.subscriptionStatus = this.currentSubscription.status;
          console.log('Current subscription:', this.currentSubscription);
        }
      },
      error: (error) => {
        console.error('Error loading current subscription', error);
        // User might not have a subscription yet
        this.currentSubscription = null;
        this.subscriptionStatus = null;
      }
    });
  }

  // Reload subscription data - can be called after events
  reloadCurrentSubscription(): void {
    this.loadCurrentSubscription();
  }

  selectPlan(plan: PlanResponseDto): void {
    // Check if subscription is ACTIVE - don't allow plan changes
    if (this.currentSubscription?.status === SubscriptionStatus.ACTIVE && this.isCurrentPlan(plan)) {
      console.log('Cannot change active plan');
      return;
    }

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

  isCurrentPlanActive(): boolean {
    return this.currentSubscription?.status === SubscriptionStatus.ACTIVE;
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
    if (plan.name === 'PROFESSIONAL') {
      return 'Popular';
    }
    if (plan.name === 'ENTERPRISE') {
      return 'Best Value';
    }
    return null;
  }

  viewPlanDetails(plan: PlanResponseDto): void {
    console.log('Viewing details for plan:', plan.name);
    const planKey = plan.name.charAt(0).toUpperCase() + plan.name.slice(1).toLowerCase();
    const planDetails = PLANS_DESCRIPTION[planKey];
    console.log('Plan key:', planKey, 'Details:', planDetails);
    if (planDetails) {
      this.selectedPlanDetails = {
        ...plan,
        ...planDetails
      };
      this.showPlanDetails = true;
      console.log('Modal opened. showPlanDetails:', this.showPlanDetails);
    } else {
      console.warn(`Plan details not found for plan: ${plan.name}`);
    }
  }

  closePlanDetails(): void {
    this.showPlanDetails = false;
    this.selectedPlanDetails = null;
  }

  toggleBilling(cycle: 'monthly' | 'annual') {
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

  getSubscriptionStatusClass(): string {
    if (!this.subscriptionStatus) return 'inactive';
    switch (this.subscriptionStatus) {
      case SubscriptionStatus.ACTIVE:
        return 'active';
      case SubscriptionStatus.PENDING:
        return 'pending';
      case SubscriptionStatus.EXPIRED:
        return 'expired';
      case SubscriptionStatus.CANCELLED:
        return 'cancelled';
      default:
        return 'inactive';
    }
  }

  getSubscriptionStatusText(): string {
    if (!this.subscriptionStatus) return 'Inactive';
    switch (this.subscriptionStatus) {
      case SubscriptionStatus.ACTIVE:
        return 'Active';
      case SubscriptionStatus.PENDING:
        return 'Pending';
      case SubscriptionStatus.EXPIRED:
        return 'Expired';
      case SubscriptionStatus.CANCELLED:
        return 'Cancelled';
      default:
        return 'Inactive';
    }
  }
}