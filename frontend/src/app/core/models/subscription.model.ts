export enum SubscriptionStatus {
  ACTIVE = 'ACTIVE',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
  PENDING = 'PENDING'
}

export enum SubscriptionDuration {
  ONE_MONTH = 'ONE_MONTH',
  THREE_MONTHS = 'THREE_MONTHS',
  TWELVE_MONTHS = 'TWELVE_MONTHS'
}

export interface SubscriptionResponseDto {
  id: number;
  status: SubscriptionStatus;
  duration: SubscriptionDuration;
  startDate: Date;
  endDate: Date;
  createdAt: Date;
  planName: string;
  price: number;
  // Optional UI field
  remainingConversions?: number;
}

export interface SubscriptionCreateDto {
  startDate: Date;
  duration: SubscriptionDuration;
  planId: number;
}

export interface SubscriptionUpdateDto {
  status: SubscriptionStatus;
}