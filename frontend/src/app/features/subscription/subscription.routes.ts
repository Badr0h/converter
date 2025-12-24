import { Routes } from '@angular/router';
import { PlansComponent } from './plans/plans.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { PaymentSuccessComponent } from './payment-success/payment-success.component';
import { PaymentFailedComponent } from './payment-failed/payment-failed.component';

export const SUBSCRIPTION_ROUTES: Routes = [
  {
    path: 'plans',
    component: PlansComponent
  },
  {
    path: 'checkout/:planId',
    component: CheckoutComponent
  },
  {
    path: 'payment-success',
    component: PaymentSuccessComponent
  },
  {
    path: 'payment-failed',
    component: PaymentFailedComponent
  },
  {
    path: '',
    redirectTo: 'plans',
    pathMatch: 'full'
  }
];