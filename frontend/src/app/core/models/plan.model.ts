import { SubscriptionDuration } from './subscription.model';

export interface PlanResponseDto {
  id: number;
  name: string;
  price: number;
  currency: string;
  duration: SubscriptionDuration;
  monthlyPrice?: number;
  annualPrice?: number;
  maxConversions?: number; // Maximum conversions per month
  features: string[];
}

export interface PlanCreateDto {
  name: string;
  price: number;
  currency: string;
  duration: SubscriptionDuration;
  maxConversions: number; // Maximum conversions per month
}