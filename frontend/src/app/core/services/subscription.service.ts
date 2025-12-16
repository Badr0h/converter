import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { SubscriptionResponseDto, SubscriptionCreateDto } from '../models/subscription.model';
import { PlanResponseDto } from '../models/plan.model';
@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private apiUrl= `${environment.apiUrl}/subscriptions`;

  constructor(private http: HttpClient) { }

  getCurrentSubscription(): Observable<SubscriptionResponseDto[]>{
    return this.http.get<SubscriptionResponseDto[]>(`${this.apiUrl}`);
  }
  getCurrentSubscriptionById(id: number): Observable<SubscriptionResponseDto[]>{
    return this.http.get<SubscriptionResponseDto[]>(`${this.apiUrl}/${id}`);
  }
  createSubscription(request: SubscriptionCreateDto): Observable<SubscriptionResponseDto>{
    return this.http.post<SubscriptionResponseDto>(`${this.apiUrl}`, request);
  }

  getPlans(): Observable<PlanResponseDto[]> {
    return this.http.get<PlanResponseDto[]>(`${environment.apiUrl}/plans`);
  }

  
}
