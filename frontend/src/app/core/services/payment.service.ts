import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaymentResponseDto, PaymentCreateDto, PayPalOrderResponse } from '../models/payment.model';

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
   * Create a simulated payment (for testing)
   */
  createSimulatedPayment(request: PaymentCreateDto): Observable<PaymentResponseDto> {
    return this.http.post<PaymentResponseDto>(`${this.apiUrl}/simulate`, request);
  }

  /**
   * Create PayPal order and get approval URL
   */
  createPayPalOrder(request: PaymentCreateDto): Observable<PayPalOrderResponse> {
    return this.http.post<PayPalOrderResponse>(`${this.apiUrl}/paypal/create`, request);
  }

  /**
   * Capture PayPal payment after user approval
   */
  capturePayPalPayment(orderId: string): Observable<PaymentResponseDto> {
    return this.http.post<PaymentResponseDto>(`${this.apiUrl}/paypal/capture/${orderId}`, {});
  }

  /**
   * Cancel a payment
   */
  cancelPayment(paymentId: number): Observable<PaymentResponseDto> {
    return this.http.put<PaymentResponseDto>(`${this.apiUrl}/${paymentId}/cancel`, {});
  }

  /**
   * Get payment by subscription ID
   */
  getPaymentBySubscriptionId(subscriptionId: number): Observable<PaymentResponseDto[]> {
    return this.http.get<PaymentResponseDto[]>(`${this.apiUrl}/subscription/${subscriptionId}`);
  }
}
