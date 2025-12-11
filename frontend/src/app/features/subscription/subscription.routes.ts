import { Routes } from '@angular/router';
import { PlansComponent } from './plans/plans.component';
import { CheckoutComponent } from './checkout/checkout.component';

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
    path: '',
    redirectTo: 'plans',
    pathMatch: 'full'
  }
];