import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';

interface Endpoint {
    method: string;
    path: string;
    status: string;
}

@Component({
    selector: 'app-admin-system',
    standalone: true,
    imports: [CommonModule, DatePipe],
    templateUrl: './admin-system.component.html',
    styleUrls: ['./admin-system.component.scss']
})
export class AdminSystemComponent implements OnInit {
    health: any = null;
    loading = true;
    now = new Date();

    endpoints: Endpoint[] = [
        { method: 'GET', path: '/api/admin/stats', status: 'OK' },
        { method: 'GET', path: '/api/admin/users', status: 'OK' },
        { method: 'GET', path: '/api/admin/conversions', status: 'OK' },
        { method: 'GET', path: '/api/conversions', status: 'OK' },
        { method: 'POST', path: '/api/conversions', status: 'OK' },
        { method: 'GET', path: '/api/subscriptions/plans', status: 'OK' },
        { method: 'POST', path: '/api/payments/paypal/create', status: 'OK' },
    ];

    constructor(private adminService: AdminService) { }

    ngOnInit(): void {
        this.loadHealth();
        this.now = new Date();
    }

    loadHealth(): void {
        this.loading = true;
        this.adminService.getSystemHealth().subscribe({
            next: (data) => {
                this.health = data;
                this.loading = false;
            },
            error: () => { this.loading = false; }
        });
    }

    getMemoryPercent(): number {
        if (!this.health?.memory) return 0;
        return Math.round((this.health.memory.used / this.health.memory.total) * 100);
    }

    formatBytes(bytes: number): string {
        if (!bytes) return '—';
        const mb = bytes / (1024 * 1024);
        return mb >= 1024 ? `${(mb / 1024).toFixed(1)} GB` : `${mb.toFixed(0)} MB`;
    }
}
