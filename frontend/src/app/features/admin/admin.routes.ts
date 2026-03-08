import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () => import('./admin-layout/admin-layout.component').then(m => m.AdminLayoutComponent),
        children: [
            {
                path: '',
                loadComponent: () => import('./admin-overview/admin-overview.component').then(m => m.AdminOverviewComponent)
            },
            {
                path: 'users',
                loadComponent: () => import('./admin-users/admin-users.component').then(m => m.AdminUsersComponent)
            },
            {
                path: 'conversions',
                loadComponent: () => import('./admin-conversions/admin-conversions.component').then(m => m.AdminConversionsComponent)
            },
            {
                path: 'system',
                loadComponent: () => import('./admin-system/admin-system.component').then(m => m.AdminSystemComponent)
            },
            {
                path: 'messages',
                loadComponent: () => import('./admin-messages/admin-messages.component').then(m => m.AdminMessagesComponent)
            }
        ]
    }
];
