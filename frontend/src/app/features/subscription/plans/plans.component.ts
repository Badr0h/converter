import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { SubscriptionResponseDto } from '../../../core/models/subscription.model';
import { PlanResponseDto } from '../../../core/models/plan.model';

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

  constructor(
    private subscriptionService: SubscriptionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPlans();
    this.loadCurrentSubscription();
  }

  loadPlans(): void {
    this.subscriptionService.getPlans().subscribe({
      next: (plans) => {
        this.plans = plans.sort((a, b) => a.price - b.price);
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
      alert('You are already subscribed to this plan!');
      return;
    }

    // Free plan - no payment needed, navigate to create subscription
    if (plan.price === 0) {
      this.router.navigate(['/subscription/checkout', plan.id]);
      return;
    }

    // Paid plan - navigate to checkout
    this.router.navigate(['/subscription/checkout', plan.id]);
  }

  isCurrentPlan(plan: PlanResponseDto): boolean {
    return this.currentSubscription?.planName === plan.name;
  }

  getPlanButtonText(plan: PlanResponseDto): string {
    if (this.isCurrentPlan(plan)) {
      return 'Current Plan';
    }
    
    if (plan.price === 0) {
      return 'Free Plan';
    }

    if (this.currentSubscription) {
      // User has a subscription: compare by price to determine upgrade/downgrade
      if (this.currentSubscription.price < plan.price) {
        return 'Upgrade';
      } else {
        return 'Downgrade';
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