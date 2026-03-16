import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { UserResponseDto } from '../models/user.model';
import { ConversionResponseDto } from '../models/conversion.model';

export interface AdminStats {
    totalUsers: number;
    totalConversions: number;
    conversionsToday: number;
    activeUsersThisWeek: number;
}

export interface DailyCount {
    day: string;
    count: number;
}

@Injectable({
    providedIn: 'root'
})
export class AdminService {
    private apiUrl = `${environment.apiUrl}/admin`;

    constructor(private http: HttpClient) { }

    // Dashboard
    getStats(): Observable<AdminStats> {
        return this.http.get<AdminStats>(`${this.apiUrl}/stats`);
    }

    getConversionsPerDay(): Observable<DailyCount[]> {
        return this.http.get<DailyCount[]>(`${this.apiUrl}/stats/conversions-per-day`);
    }

    getRecentActivity(): Observable<ConversionResponseDto[]> {
        return this.http.get<ConversionResponseDto[]>(`${this.apiUrl}/stats/recent-activity`);
    }

    // Users
    getAllUsers(): Observable<UserResponseDto[]> {
        return this.http.get<UserResponseDto[]>(`${this.apiUrl}/users`);
    }

    createUser(user: any): Observable<UserResponseDto> {
        return this.http.post<UserResponseDto>(`${this.apiUrl}/users`, user);
    }

    updateUserRole(userId: number, role: string): Observable<UserResponseDto> {
        return this.http.patch<UserResponseDto>(`${this.apiUrl}/users/${userId}/role`, { role });
    }

    toggleUserEnabled(userId: number): Observable<UserResponseDto> {
        return this.http.patch<UserResponseDto>(`${this.apiUrl}/users/${userId}/toggle-enabled`, {});
    }

    deleteUser(userId: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/users/${userId}`);
    }

    getUserConversions(userId: number): Observable<ConversionResponseDto[]> {
        return this.http.get<ConversionResponseDto[]>(`${this.apiUrl}/users/${userId}/conversions`);
    }

    // Conversions
    getAllConversions(): Observable<ConversionResponseDto[]> {
        return this.http.get<ConversionResponseDto[]>(`${this.apiUrl}/conversions`);
    }

    deleteConversion(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/conversions/${id}`);
    }

    // System Health
    getSystemHealth(): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/health`);
    }
}
