import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaymentResponseDto, PaymentCreateDto } from '../models/payment.model';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = `${environment.apiUrl}/payments`;

  constructor(private http: HttpClient) { }

  /**
   * Get all payments for the current user
   */
  getAllPayments(): Observable<PaymentResponseDto[]> {
    return this.http.get<PaymentResponseDto[]>(`${this.apiUrl}`);
  }

  /**
   * Get payment by ID
   */
  getPaymentById(id: number): Observable<PaymentResponseDto> {
    return this.http.get<PaymentResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Create a new payment
   */
  createPayment(request: PaymentCreateDto): Observable<PaymentResponseDto> {
    return this.http.post<PaymentResponseDto>(`${this.apiUrl}`, request);
  }

  /**
   * Get payment by subscription ID
   */
  getPaymentBySubscriptionId(subscriptionId: number): Observable<PaymentResponseDto[]> {
    return this.http.get<PaymentResponseDto[]>(`${this.apiUrl}/subscription/${subscriptionId}`);
  }
}
