export interface PlanResponseDto {
  id: number;
  name: string;
  price: number;
  currency: string;
  duration: number;
}

export interface PlanCreateDto {
  name: string;
  price: number;
  currency: string;
  duration: number;
}