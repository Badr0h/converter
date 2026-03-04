import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/user.model';

export const adminGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const authService = inject(AuthService);

    if (!authService.isAuthenticated()) {
        router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
        return false;
    }

    const currentUser = authService.currentUserValue;
    if (currentUser?.role === UserRole.ADMIN) {
        return true;
    }

    // Not an admin, redirect to dashboard (403-like)
    router.navigate(['/dashboard']);
    return false;
};
