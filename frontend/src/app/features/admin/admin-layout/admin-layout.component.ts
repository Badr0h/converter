import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserResponseDto } from '../../../core/models/user.model';
import { filter } from 'rxjs/operators';

@Component({
    selector: 'app-admin-layout',
    standalone: true,
    imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
    templateUrl: './admin-layout.component.html',
    styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit {
    currentUser: UserResponseDto | null = null;
    sidebarCollapsed = false;
    mobileMenuOpen = false;
    currentRoute = '';

    constructor(private authService: AuthService, private router: Router) { }

    ngOnInit(): void {
        this.authService.currentUser.subscribe(user => {
            this.currentUser = user;
        });

        this.router.events.pipe(
            filter(e => e instanceof NavigationEnd)
        ).subscribe((e: any) => {
            this.currentRoute = e.urlAfterRedirects;
            this.mobileMenuOpen = false;
        });
    }

    toggleSidebar(): void {
        this.sidebarCollapsed = !this.sidebarCollapsed;
    }

    toggleMobileSidebar(): void {
        this.mobileMenuOpen = !this.mobileMenuOpen;
    }

    closeMobileSidebar(): void {
        this.mobileMenuOpen = false;
    }

    closeOnMobile(): void {
        this.mobileMenuOpen = false;
    }

    getPageTitle(): string {
        if (this.currentRoute.includes('/admin/users')) return 'User Management';
        if (this.currentRoute.includes('/admin/conversions')) return 'Conversions History';
        if (this.currentRoute.includes('/admin/messages')) return 'Contact Messages';
        if (this.currentRoute.includes('/admin/system')) return 'System Health';
        return 'Admin Dashboard';
    }
}
