import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-payment-failed',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-failed.component.html',
  styleUrls: ['./payment-failed.component.scss']
})
export class PaymentFailedComponent implements OnInit {

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Log the payment failure for analytics
    console.log('Payment failed - user returned to failure page');
  }

  retryPayment(): void {
    this.router.navigate(['/subscription/plans']);
  }

  goToSupport(): void {
    this.router.navigate(['/contact']);
  }
}
