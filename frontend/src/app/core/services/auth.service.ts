import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from 'src/environments/environment';
import { AuthRequest, AuthResponse } from '../models/auth.model';
import { UserCreateDto, UserResponseDto } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject: BehaviorSubject<UserResponseDto | null>;
  public currentUser: Observable<UserResponseDto | null>;

  constructor(private http: HttpClient, private router: Router) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<UserResponseDto | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): UserResponseDto | null {
    return this.currentUserSubject.value;
  }

// Dans auth.service.ts

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {
          // 1. Stocker le jeton
          localStorage.setItem('token', response.token); 
          // 2. Stocker le refreshToken
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
          // 3. Stocker l'utilisateur dans le localStorage
          localStorage.setItem('currentUser', JSON.stringify(response.user));
          // 4. Mettre à jour le BehaviorSubject pour notifier les autres composants
          this.currentUserSubject.next(response.user as unknown as UserResponseDto); 
        })
      );
  }

  register(data: UserCreateDto): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data);
  }

  verifyEmail(request: {email: string, code: string}): Observable<{message: string}> {
    return this.http.post<{message: string}>(`${this.apiUrl}/verify-email`, request);
  }

  resendVerificationCode(email: string): Observable<{message: string}> {
    return this.http.post<{message: string}>(`${this.apiUrl}/resend-verification`, {email});
  }

  logout(): void {
      localStorage.removeItem('currentUser');
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      
      this.currentUserSubject.next(null);
      this.router.navigate(['/auth/login']);
  }

  isAuthenticated(): boolean {
    return !!this.currentUserValue && !!this.getToken();
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          localStorage.setItem('token', response.token);
        })
      );  
      
        
  }
}