import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { PaymentService } from '../../../core/services/payment.service';
import { SubscriptionResponseDto, SubscriptionCreateDto, SubscriptionDuration } from '../../../core/models/subscription.model';
import { PaymentCreateDto, PaymentResponseDto } from '../../../core/models/payment.model';
import { PlanResponseDto } from '../../../core/models/plan.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  checkoutForm!: FormGroup;
  plan: PlanResponseDto | null = null;
  planId: number = 0;
  loading = false;
  processing = false;
  errorMessage = '';
  billingCycle: 'monthly' | 'annual' = 'monthly';
  selectedPaymentMethod: 'card' | 'paypal' = 'card';

  constructor(
    private formBuilder: FormBuilder,
    private subscriptionService: SubscriptionService,
    private paymentService: PaymentService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadPlan();
    const billing = this.route.snapshot.queryParamMap.get('billing');
    if (billing === 'annual') this.billingCycle = 'annual';
    
    // Initialize validators for default payment method
    this.selectPaymentMethod(this.selectedPaymentMethod);
  }

  private mapPlanDurationToEnum(durationValue: number | string) {
    // Backend plan.duration may be stored as months OR as days (e.g. 30, 90, 365).
    const n = Number(durationValue);
    if (isNaN(n)) return String(durationValue);

    // Direct month values
    if (n === 1 || n === 30) return 'ONE_MONTH';
    if (n === 3 || n === 90) return 'THREE_MONTHS';
    if (n === 12 || n === 365) return 'TWELVE_MONTHS';

    // Heuristic fallbacks (in case plan.duration uses other day counts)
    if (n >= 360) return 'TWELVE_MONTHS';
    if (n >= 80) return 'THREE_MONTHS';
    if (n >= 25) return 'ONE_MONTH';

    // Final fallback
    return 'ONE_MONTH';
  }

  initForm(): void {
    this.checkoutForm = this.formBuilder.group({
      // Accept either 16 contiguous digits or groups of 4 separated by spaces (e.g. "4552 5670 1500 8958")
      cardNumber: [''],
      cardName: [''],
      expiryDate: [''],
      cvv: [''],
      billingAddress: [''],
      city: [''],
      zipCode: [''],
      country: ['USA'],
      paymentMethod: ['card', Validators.required]
    });
  }

  loadPlan(): void {
    this.planId = Number(this.route.snapshot.paramMap.get('planId'));
    
    if (!this.planId) {
      this.errorMessage = 'Invalid plan selected';
      return;
    }

    this.loading = true;

    this.subscriptionService.getPlans().subscribe({
      next: (plans) => {
        this.plan = plans.find(p => p.id === this.planId) || null;
        if (!this.plan) {
          this.errorMessage = 'Plan not found';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading plan', error);
        this.errorMessage = 'Failed to load plan details';
        this.loading = false;

      }
    });
  }

  
  get f() {
    return this.checkoutForm.controls;
  }

  formatCardNumber(event: any): void {
    let value = event.target.value.replace(/\s/g, '');
    let formattedValue = value.match(/.{1,4}/g)?.join(' ') || value;
    this.checkoutForm.patchValue({ cardNumber: formattedValue }, { emitEvent: false });
  }

  formatExpiryDate(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    if (value.length >= 2) {
      value = value.substring(0, 2) + '/' + value.substring(2, 4);
    }
    this.checkoutForm.patchValue({ expiryDate: value }, { emitEvent: false });
  }

  private localDateString(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  onSubmit(): void {
    if (!this.plan) {
      console.error('No plan selected');
      return;
    }

    // Only validate card fields if card payment is selected
    if (this.selectedPaymentMethod === 'card' && this.checkoutForm.invalid) {
      console.log('Form validation failed for card payment');
      return;
    }

    this.processing = true;
    this.errorMessage = '';

    // Create subscription first (status will be PENDING)
    const subscriptionRequest: SubscriptionCreateDto = {
      planId: this.plan!.id,
      // Send ISO LocalDate (yyyy-MM-dd) so backend's LocalDate binds correctly
      startDate: this.localDateString(new Date()) as any,
      // Backend expects SubscriptionDuration enum names (ONE_MONTH, THREE_MONTHS, TWELVE_MONTHS)
      duration: this.mapPlanDurationToEnum(this.plan!.duration as any) as any
    };

    this.subscriptionService.createSubscription(subscriptionRequest).subscribe({
      next: (createdSubscription) => {
        if (this.selectedPaymentMethod === 'paypal') {
          this.processPayPalPayment(createdSubscription);
        } else {
          this.processCardPayment(createdSubscription);
        }
      },
      error: (error) => {
        console.error('Subscription creation error', error);
        this.errorMessage = 'Failed to create subscription. Please try again.';
        this.processing = false;
      }
    });
  }

  private processCardPayment(createdSubscription: any): void {
    // Then create payment linked to the subscription
    const paymentRequest: PaymentCreateDto = {
      userId: this.authService.currentUserValue?.id || 0,
      subscriptionId: createdSubscription.id,
      paymentMethod: 'card',
      paymentToken: 'SIM-' + Date.now(),
      billingCycle: this.billingCycle
    };

    this.paymentService.createSimulatedPayment(paymentRequest).subscribe({
      next: (paymentResponse) => {
        console.log('Payment successful', paymentResponse);
        // After payment is successful, activate the subscription on the backend
        this.subscriptionService.activateSubscription(createdSubscription.id).subscribe({
          next: (activated) => {
            console.log('Subscription activated', activated);
            this.processing = false;
            // Wait a moment for database to fully update, then navigate
            setTimeout(() => {
              this.router.navigate(['/subscription/plans'], { queryParams: { refresh: Date.now() } });
            }, 500);
          },
          error: (actErr) => {
            console.error('Activation error', actErr);
            this.errorMessage = 'Payment succeeded but activation failed. Please contact support.';
            this.processing = false;
          }
        });
      },
      error: (error) => {
        console.error('Payment error', error);
        this.errorMessage = error.error?.message || 'Payment failed. Please check your card details.';
        this.processing = false;
      }
    });
  }

  private processPayPalPayment(createdSubscription: any): void {
    const paymentRequest: PaymentCreateDto = {
      userId: this.authService.currentUserValue?.id || 0,
      subscriptionId: createdSubscription.id,
      paymentMethod: 'paypal',
      billingCycle: this.billingCycle
    };

    this.paymentService.createPayPalOrder(paymentRequest).subscribe({
      next: (response) => {
        console.log('PayPal order created', response);
        // Redirect to PayPal for approval
        window.location.href = response.approvalUrl;
      },
      error: (error) => {
        console.error('PayPal order creation error', error);
        this.errorMessage = error.error?.message || 'Failed to create PayPal order. Please try again.';
        this.processing = false;
      }
    });
  }

  selectPaymentMethod(method: 'card' | 'paypal'): void {
    this.selectedPaymentMethod = method;
    this.checkoutForm.patchValue({ paymentMethod: method });
    
    // Update validators based on payment method
    if (method === 'card') {
      // Add validators for card fields
      this.checkoutForm.get('cardNumber')?.setValidators([Validators.required, Validators.pattern(/^(?:\d{4}\s){3}\d{4}$|^\d{16}$/)]);
      this.checkoutForm.get('cardName')?.setValidators([Validators.required, Validators.minLength(3)]);
      this.checkoutForm.get('expiryDate')?.setValidators([Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]);
      this.checkoutForm.get('cvv')?.setValidators([Validators.required, Validators.pattern(/^\d{3,4}$/)]);
      this.checkoutForm.get('billingAddress')?.setValidators(Validators.required);
      this.checkoutForm.get('city')?.setValidators(Validators.required);
      this.checkoutForm.get('zipCode')?.setValidators([Validators.required, Validators.pattern(/^\d{5}$/)]);
      this.checkoutForm.get('country')?.setValidators(Validators.required);
    } else {
      // Remove validators for card fields when using PayPal
      this.checkoutForm.get('cardNumber')?.clearValidators();
      this.checkoutForm.get('cardName')?.clearValidators();
      this.checkoutForm.get('expiryDate')?.clearValidators();
      this.checkoutForm.get('cvv')?.clearValidators();
      this.checkoutForm.get('billingAddress')?.clearValidators();
      this.checkoutForm.get('city')?.clearValidators();
      this.checkoutForm.get('zipCode')?.clearValidators();
      this.checkoutForm.get('country')?.clearValidators();
    }
    
    // Update form status
    this.checkoutForm.get('cardNumber')?.updateValueAndValidity();
    this.checkoutForm.get('cardName')?.updateValueAndValidity();
    this.checkoutForm.get('expiryDate')?.updateValueAndValidity();
    this.checkoutForm.get('cvv')?.updateValueAndValidity();
    this.checkoutForm.get('billingAddress')?.updateValueAndValidity();
    this.checkoutForm.get('city')?.updateValueAndValidity();
    this.checkoutForm.get('zipCode')?.updateValueAndValidity();
    this.checkoutForm.get('country')?.updateValueAndValidity();
  }

  isCardPaymentRequired(): boolean {
    return this.selectedPaymentMethod === 'card';
  }
  cancel(): void {
    if (confirm('Are you sure you want to cancel?')) {
      this.router.navigate(['/subscription/plans']);
    }
  }

  calculateTotal(): number {
    // Default to monthly price for checkout total if available
    if (!this.plan) return 0;
    return this.billingCycle === 'annual'
      ? (this.plan.annualPrice ?? ((this.plan.monthlyPrice ?? this.plan.price) * 10))
      : (this.plan.monthlyPrice ?? this.plan.price ?? 0);
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }

}

