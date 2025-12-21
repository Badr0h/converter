import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ConversionService } from '../../../core/services/conversion.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { UserResponseDto } from '../../../core/models/user.model';
import { ConversionResponseDto } from '../../../core/models/conversion.model';
import { SubscriptionResponseDto } from '../../../core/models/subscription.model';
import { DashboardStatsDto } from '../../../core/models/dashboard-stats.model';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './user-dashboard.component.html',
  styleUrl: './user-dashboard.component.scss'
})
export class UserDashboardComponent implements OnInit {
  currentUser: UserResponseDto | null = null;
  recentConversions: ConversionResponseDto[] = [];
  subscription: SubscriptionResponseDto | null = null;
  loading = true;
  statsLoading = true;
  dashboardStats: DashboardStatsDto | null = null;

  constructor(
    private authService: AuthService,
    private conversionService: ConversionService,
    private subscriptionService: SubscriptionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUserData();
    this.loadRecentConversions();
    this.loadDashboardStats();
    this.loadSubscriptionInfo();
  }

  loadUserData(): void {
    this.currentUser = this.authService.currentUserValue;
  }

  loadDashboardStats(): void {
    this.subscriptionService.getDashboardStats().subscribe({
      next: (stats) => {
        this.dashboardStats = stats;
        this.statsLoading = false;
      },
      error: (error) => {
        console.error('Error loading dashboard stats', error);
        // Set defaults if stats not found
        this.dashboardStats = {
          totalConversions: 0,
          remainingConversions: 1,
          subscriptionStatus: 'FREE',
          maxConversionsPerMonth: 1
        };
        this.statsLoading = false;
      }
    });
  }

  loadRecentConversions(): void {
    this.conversionService.getConversionHistory().subscribe({
      next: (conversions) => {
        // Sort by date descending and get last 5
        this.recentConversions = conversions
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 5);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading conversions', error);
        this.loading = false;
      }
    });
  }

  loadSubscriptionInfo(): void {
    this.subscriptionService.getCurrentSubscription().subscribe({
      next: (subscription) => {
        this.subscription = subscription[0];
        this.statsLoading = false;
      },
      error: (error) => {
        console.error('Error loading subscription', error);
        this.statsLoading = false;
      }
    });
  }

  navigateToConverter(): void {
    this.router.navigate(['/conversion']);
  }

  navigateToPlans(): void {
    this.router.navigate(['/subscription/plans']);
  }

  viewConversion(conversion: ConversionResponseDto): void {
    // Navigate to converter with query param to view this conversion
    this.router.navigate(['/conversion'], { 
      queryParams: { view: conversion.id } 
    });
  }

  copyAiResponse(aiResponse: string): void {
    if (!aiResponse) {
      alert('No response to copy');
      return;
    }
    
    navigator.clipboard.writeText(aiResponse).then(() => {
      alert('Response copied to clipboard! ✓');
    }).catch(err => {
      console.error('Failed to copy:', err);
      alert('Failed to copy to clipboard');
    });
  }

  deleteConversion(id: number): void {
    if (confirm('Are you sure you want to delete this conversion?')) {
      this.conversionService.deleteConversion(id).subscribe({
        next: () => {
          this.recentConversions = this.recentConversions.filter(c => c.id !== id);
          // Refresh dashboard stats to update conversion count
          this.loadDashboardStats();
          alert('Conversion deleted successfully!');
        },
        error: (error) => {
          console.error('Error deleting conversion', error);
          alert('Failed to delete conversion');
        }
      });
    }
  }

  logout(): void {
    this.authService.logout();
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  truncateText(text: string, maxLength: number = 100): string {
    if (!text) return 'No content';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }
}