import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService, AdminStats, DailyCount } from '../../../core/services/admin.service';
import { ConversionResponseDto } from '../../../core/models/conversion.model';

@Component({
    selector: 'app-admin-overview',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './admin-overview.component.html',
    styleUrls: ['./admin-overview.component.scss']
})
export class AdminOverviewComponent implements OnInit {
    stats: AdminStats | null = null;
    chartData: DailyCount[] = [];
    recentActivity: ConversionResponseDto[] = [];
    activityLoading = true;
    maxCount = 1;

    constructor(private adminService: AdminService) { }

    ngOnInit(): void {
        this.adminService.getStats().subscribe({
            next: (s) => this.stats = s,
            error: () => { }
        });

        this.adminService.getConversionsPerDay().subscribe({
            next: (data) => {
                this.chartData = data;
                this.maxCount = Math.max(1, ...data.map(d => d.count));
            },
            error: () => { }
        });

        this.adminService.getRecentActivity().subscribe({
            next: (data) => {
                this.recentActivity = data;
                this.activityLoading = false;
            },
            error: () => { this.activityLoading = false; }
        });
    }

    getBarHeight(count: number): number {
        return Math.max(4, (count / this.maxCount) * 100);
    }

    formatChartDay(day: string): string {
        const d = new Date(day);
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }

    formatDate(date: Date | string): string {
        const d = typeof date === 'string' ? new Date(date) : date;
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }
}
