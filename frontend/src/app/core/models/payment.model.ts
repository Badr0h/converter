export enum PaymentStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
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
}

export interface PaymentCreateDto {
  userId: number;
  subscriptionId: number;
  paymentMethod: string;
  paymentToken: string;
  billingCycle?: 'monthly' | 'annual' | string;
}