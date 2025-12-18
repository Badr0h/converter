import { SubscriptionDuration } from './subscription.model';

export interface PlanResponseDto {
  id: number;
  name: string;
  price: number;
  currency: string;
  duration: SubscriptionDuration;
  monthlyPrice?: number;
  annualPrice?: number;
  // Optional UI fields (may be absent from backend)
  maxConversions?: number;
  features?: string[];
}

export interface PlanCreateDto {
  name: string;
  price: number;
  currency: string;
  duration: SubscriptionDuration;
}