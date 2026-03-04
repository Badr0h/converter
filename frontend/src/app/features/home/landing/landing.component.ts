import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { PlanResponseDto } from '../../../core/models/plan.model';

interface Feature {
  icon: string;
  title: string;
  description: string;
}

interface SupportedFormat {
  name: string;
  symbol: string;
  color: string;
}

interface HowItWorksStep {
  number: number;
  title: string;
  description: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent implements OnInit, OnDestroy {

  // Pricing
  pricingPlans: PlanResponseDto[] = [];
  isLoadingPricing = false;
  pricingError = false;

  // Features
  readonly features: Feature[] = [
    {
      icon: '🔄',
      title: 'AI-Powered Conversion',
      description: 'Convert between any text format using advanced AI technology. From JSON to XML, Python to Java, and more.'
    },
    {
      icon: '⚡',
      title: 'Lightning Fast',
      description: 'Get instant conversions powered by state-of-the-art language models. No waiting, no hassle.'
    },
    {
      icon: '🎯',
      title: 'High Accuracy',
      description: 'Our AI understands context and structure to provide accurate, reliable conversions every time.'
    },
    {
      icon: '🔒',
      title: 'Secure & Private',
      description: 'Your data is encrypted and never stored. Complete privacy and security guaranteed.'
    },
    {
      icon: '📚',
      title: 'Multiple Formats',
      description: 'Support for programming languages, data formats, markup languages, and more.'
    },
    {
      icon: '💎',
      title: 'Premium Features',
      description: 'Unlimited conversions, API access, priority support, and advanced AI models.'
    }
  ];

  // Supported Formats
  readonly supportedFormats: SupportedFormat[] = [
    { name: 'JSON', symbol: '{ }', color: '#f7df1e' },
    { name: 'XML', symbol: '< >', color: '#e44d26' },
    { name: 'Python', symbol: 'py', color: '#3776ab' },
    { name: 'Java', symbol: 'J', color: '#007396' },
    { name: 'JavaScript', symbol: 'JS', color: '#f7df1e' },
    { name: 'TypeScript', symbol: 'TS', color: '#3178c6' },
    { name: 'SQL', symbol: '⚡', color: '#00758f' },
    { name: 'YAML', symbol: 'YML', color: '#cb171e' },
    { name: 'CSV', symbol: '📊', color: '#217346' },
    { name: 'HTML', symbol: '<h>', color: '#e44d26' },
    { name: 'Markdown', symbol: 'MD', color: '#000000' },
    { name: 'C++', symbol: 'C++', color: '#00599c' }
  ];

  // Math Symbols for Background Animation
  readonly mathSymbols: string[] = [
    '∫', '∑', '∏', '√', '∞', 'π', 'Δ', 'Ω', 'λ', '∂', '∇', 'α', 'β', 'γ'
  ];

  // How It Works Steps
  readonly howItWorksSteps: HowItWorksStep[] = [
    {
      number: 1,
      title: 'Paste Your Code',
      description: 'Simply paste your code or data in the input area'
    },
    {
      number: 2,
      title: 'Select Format',
      description: 'Choose the target format you want to convert to'
    },
    {
      number: 3,
      title: 'Get Results',
      description: 'Receive your converted code instantly, powered by AI'
    }
  ];

  // Stats for Hero Section
  readonly stats = [
    { number: '50K+', label: 'Conversions' },
    { number: '12+', label: 'Formats' },
    { number: '99.9%', label: 'Accuracy' }
  ];

  // Destroy subject for cleanup
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly subscriptionService: SubscriptionService,
    private readonly title: Title,
    private readonly meta: Meta
  ) { }

  ngOnInit(): void {
    this.applySeo();
    this.loadPricingPlans();
    this.preloadCriticalData();
  }

  private applySeo(): void {
    const title = 'AI Converter - Convert Code & Data Formats Instantly';
    const description = 'AI Converter: Instant code and data format conversion via AI. Transform JSON, XML, SQL, Python & more with fast, accurate results.';
    const url = 'https://aiconverter.com/';
    const image = 'https://aiconverter.com/assets/og-image.png';

    this.title.setTitle(title);

    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ name: 'robots', content: 'index,follow' });

    // Open Graph
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:image', content: image });

    // Twitter
    this.meta.updateTag({ property: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ property: 'twitter:url', content: url });
    this.meta.updateTag({ property: 'twitter:title', content: title });
    this.meta.updateTag({ property: 'twitter:description', content: description });
    this.meta.updateTag({ property: 'twitter:image', content: image });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadPricingPlans(): void {
    this.isLoadingPricing = true;
    this.pricingError = false;

    this.subscriptionService.getPlans()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (plans) => {
          this.pricingPlans = this.sortPlans(plans);
          this.isLoadingPricing = false;
        },
        error: (error) => {
          console.error('Error loading pricing plans:', error);
          this.pricingError = true;
          this.isLoadingPricing = false;
          // Fallback to default plans if API fails
          this.pricingPlans = this.getDefaultPlans();
        }
      });
  }

  private sortPlans(plans: PlanResponseDto[]): PlanResponseDto[] {
    // Sort plans by price ascending
    return plans.sort((a, b) => a.price - b.price);
  }

  private getDefaultPlans(): PlanResponseDto[] {
    return [
      {
        id: 1,
        name: 'STARTER',
        price: 15.00,
        currency: 'USD',
        duration: 'ONE_MONTH' as any,
        maxConversions: 300,
        features: ['300 conversions/mo', 'Fast AI processing', 'Standard support']
      },
      {
        id: 2,
        name: 'PROFESSIONAL',
        price: 39.00,
        currency: 'USD',
        duration: 'ONE_MONTH' as any,
        maxConversions: 1500,
        features: ['1,500 conversions/mo', 'Priority processing', 'Batch tools']
      },
      {
        id: 3,
        name: 'ENTERPRISE',
        price: 89.00,
        currency: 'USD',
        duration: 'ONE_MONTH' as any,
        maxConversions: -1, // Unlimited
        features: ['Unlimited conversions', 'API access', '24/7 VIP support']
      }
    ];
  }

  private preloadCriticalData(): void {
    // Preload any critical data or assets
    // This could include checking authentication status, loading user preferences, etc.
  }

  // Navigation Methods
  navigateToRegister(): void {
    this.router.navigate(['/auth/register']).catch(error => {
      console.error('Navigation error:', error);
    });
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']).catch(error => {
      console.error('Navigation error:', error);
    });
  }

  navigateToDashboard(): void {
    this.router.navigate(['/dashboard']).catch(error => {
      console.error('Navigation error:', error);
    });
  }

  navigateToConverter(): void {
    this.router.navigate(['/converter']).catch(error => {
      console.error('Navigation error:', error);
    });
  }

  // Authentication Check
  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  // Smooth Scroll Methods
  scrollToFeatures(): void {
    this.smoothScrollTo('features');
  }

  scrollToPricing(): void {
    this.smoothScrollTo('pricing');
  }

  scrollToHowItWorks(): void {
    this.smoothScrollTo('how-it-works');
  }

  private smoothScrollTo(elementId: string): void {
    const element = document.getElementById(elementId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      console.warn(`Element with ID '${elementId}' not found`);
    }
  }

  // Plan Helpers
  getPlanFeatures(plan: PlanResponseDto): string[] {
    const baseFeatures = [
      `${plan.maxConversions === -1 ? 'Unlimited' : plan.maxConversions} conversions/${this.formatDuration(plan.duration)}`,
      'AI-powered conversion',
      'Multiple format support'
    ];

    if (plan.name === 'PROFESSIONAL' || plan.name === 'ENTERPRISE') {
      baseFeatures.push('Priority support', 'Advanced AI models');
    }

    if (plan.name === 'ENTERPRISE') {
      baseFeatures.push('API access', 'Custom integrations');
    }

    return baseFeatures;
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  formatDuration(duration: string): string {
    const durationMap: Record<string, string> = {
      'ONE_MONTH': 'month',
      'THREE_MONTHS': '3 months',
      'SIX_MONTHS': '6 months',
      'TWELVE_MONTHS': 'year'
    };
    return durationMap[duration] || 'month';
  }

  isPlanPopular(plan: PlanResponseDto): boolean {
    return plan.name === 'PROFESSIONAL';
  }

  // TrackBy Functions for Performance
  trackByFormatName(index: number, format: SupportedFormat): string {
    return format.name;
  }

  trackByFeatureTitle(index: number, feature: Feature): string {
    return feature.title;
  }

  trackByStepNumber(index: number, step: HowItWorksStep): number {
    return step.number;
  }

  trackByPlanId(index: number, plan: PlanResponseDto): number {
    return plan.id;
  }

  trackBySymbol(index: number, symbol: string): string {
    return symbol;
  }

  trackByStatLabel(index: number, stat: { number: string; label: string }): string {
    return stat.label;
  }

  // Call to Action
  handleGetStarted(): void {
    if (this.isAuthenticated()) {
      this.navigateToConverter();
    } else {
      this.navigateToRegister();
    }
  }

  // Plan Selection
  selectPlan(plan: PlanResponseDto): void {
    if (!this.isAuthenticated()) {
      // Store selected plan in session and redirect to register
      sessionStorage.setItem('selectedPlan', JSON.stringify(plan));
      this.navigateToRegister();
    } else {
      // Navigate to subscription/checkout page
      this.router.navigate(['/subscription/checkout'], {
        queryParams: { planId: plan.id }
      }).catch(error => {
        console.error('Navigation error:', error);
      });
    }
  }

  retryLoadingPlans(): void {
    this.loadPricingPlans();
  }

  // Get current year for footer
  get currentYear(): number {
    return new Date().getFullYear();
  }
}