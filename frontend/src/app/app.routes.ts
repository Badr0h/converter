import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { LandingComponent } from './features/home/landing/landing.component';

export const routes: Routes = [
  {
    path: '',
    component: LandingComponent  // Home page - accessible to everyone
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES),
    canActivate: [authGuard]  // Protected - requires login
  },
  {
    path: 'conversion',
    loadChildren: () => import('./features/conversion/conversion.routes').then(m => m.CONVERSION_ROUTES),
    canActivate: [authGuard]  // Protected - requires login
  },
  {
    path: 'subscription',
    loadChildren: () => import('./features/subscription/subscription.routes').then(m => m.SUBSCRIPTION_ROUTES),
    canActivate: [authGuard]  // Protected - requires login
  },
  {
    path: 'profile',
    loadChildren: () => import('./features/profile/profile.routes').then(m => m.CONVERSION_ROUTES),
    canActivate: [authGuard]  // Protected - requires login
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES),
    canActivate: [adminGuard]  // Protected - requires ADMIN role
  },
  {
    path: '**',
    redirectTo: ''  // Redirect unknown routes to home
  }
];