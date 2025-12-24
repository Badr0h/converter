export enum PaymentStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED'
}

export interface PaymentResponseDto {
  id: number;
  userId: number;
  subscriptionId: number;
  amount: number;
  currency: string;
  status: PaymentStatus;
  paymentMethod: string;
  transactionId: string;
  createdAt: Date;
  updatedAt?: Date;
}

export interface PaymentCreateDto {
  userId: number;
  subscriptionId: number;
  paymentMethod: string;
  paymentToken?: string;
  billingCycle?: 'monthly' | 'annual' | string;
}

export interface PayPalOrderResponse {
  approvalUrl: string;
}