import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../../core/services/payment.service';
import { SubscriptionService } from '../../../core/services/subscription.service';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-success.component.html',
  styleUrls: ['./payment-success.component.scss']
})
export class PaymentSuccessComponent implements OnInit {
  loading = true;
  success = false;
  errorMessage = '';
  paymentDetails: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private subscriptionService: SubscriptionService
  ) {}

  ngOnInit(): void {
    this.handlePayPalReturn();
  }

  private handlePayPalReturn(): void {
    const orderId = this.route.snapshot.queryParamMap.get('token');
    const payerId = this.route.snapshot.queryParamMap.get('PayerID');
    
    if (!orderId) {
      this.errorMessage = 'No payment token found. Please contact support.';
      this.loading = false;
      return;
    }

    this.paymentService.capturePayPalPayment(orderId).subscribe({
      next: (paymentResponse) => {
        console.log('PayPal payment captured successfully', paymentResponse);
        this.paymentDetails = paymentResponse;
        
        // Activate the subscription
        this.subscriptionService.activateSubscription(paymentResponse.subscriptionId).subscribe({
          next: (activated) => {
            console.log('Subscription activated', activated);
            this.success = true;
            this.loading = false;
          },
          error: (actErr) => {
            console.error('Subscription activation error', actErr);
            this.errorMessage = 'Payment was successful but there was an issue activating your subscription. Please contact support.';
            this.loading = false;
          }
        });
      },
      error: (error) => {
        console.error('PayPal payment capture error', error);
        this.errorMessage = error.error?.message || 'Failed to capture PayPal payment. Please contact support.';
        this.loading = false;
      }
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/subscription/plans'], { queryParams: { refresh: Date.now() } });
  }

  goToSupport(): void {
    this.router.navigate(['/contact']);
  }
}
