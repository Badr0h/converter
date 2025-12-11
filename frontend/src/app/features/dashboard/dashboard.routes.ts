import { Routes } from '@angular/router';
import { UserDashboardComponent } from './user-dashboard/user-dashboard.component';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    component: UserDashboardComponent
  }
];