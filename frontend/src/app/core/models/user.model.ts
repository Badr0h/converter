export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN'
}

export interface UserCreateDto {
  fullName: string;
  email: string;
  password: string;
}

export interface UserResponseDto {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  createdAt: Date;
  updatedAt: Date;
}

export interface UserUpdateDto {
  fullName: string;
  email: string;
  password: string;
}