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
  }

  initForm(): void {
    this.checkoutForm = this.formBuilder.group({
      cardNumber: ['', [Validators.required, Validators.pattern(/^\d{16}$/)]],
      cardName: ['', [Validators.required, Validators.minLength(3)]],
      expiryDate: ['', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]],
      cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
      billingAddress: ['', Validators.required],
      city: ['', Validators.required],
      zipCode: ['', [Validators.required, Validators.pattern(/^\d{5}$/)]],
      country: ['USA', Validators.required]
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

  onSubmit(): void {
    if (this.checkoutForm.invalid || !this.plan) {
      return;
    }

    this.processing = true;
    this.errorMessage = '';

    // Create subscription first (status will be PENDING)
    const subscriptionRequest: SubscriptionCreateDto = {
      planId: this.plan!.id,
      startDate: new Date(),
      duration: this.plan!.duration as SubscriptionDuration
    };

    this.subscriptionService.createSubscription(subscriptionRequest).subscribe({
      next: (createdSubscription) => {
        // Then create payment linked to the subscription
        const paymentRequest: PaymentCreateDto = {
          userId: this.authService.currentUserValue?.id || 0,
          subscriptionId: createdSubscription.id,
          paymentMethod: 'card',
          paymentToken: 'SIM-' + Date.now()
        };

        this.paymentService.createPayment(paymentRequest).subscribe({
          next: (paymentResponse) => {
            console.log('Payment successful', paymentResponse);
            this.processing = false;
            alert('✓ Subscription created (pending). Payment processed successfully.');
            this.router.navigate(['/dashboard']);
          },
          error: (error) => {
            console.error('Payment error', error);
            this.errorMessage = error.error?.message || 'Payment failed. Please check your card details.';
            this.processing = false;
          }
        });
      },
      error: (error) => {
        console.error('Subscription creation error', error);
        this.errorMessage = 'Failed to create subscription. Please try again.';
        this.processing = false;
      }
    });
  }
  cancel(): void {
    if (confirm('Are you sure you want to cancel?')) {
      this.router.navigate(['/subscription/plans']);
    }
  }

  calculateTotal(): number {
    return this.plan ? this.plan.price : 0;
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }

}

