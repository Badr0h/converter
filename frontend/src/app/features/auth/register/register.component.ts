import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthRequest } from '../../../core/models/auth.model';
import { UserCreateDto } from "../../../core/models/user.model";

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';
  passwordRequirements = {
    minLength: false,
    hasUppercase: false,
    hasLowercase: false,
    hasNumber: false,
    hasSpecialChar: false
  };

  showPassword = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Initialize the form with validation
    this.registerForm = this.formBuilder.group({
      fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20), Validators.pattern(/^[a-zA-Z\s]+$/)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128), Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])[A-Za-z0-9@$!%*?&]{8,}$/)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });

    // Update password requirements on input
    this.registerForm.get('password')?.valueChanges.subscribe(value => {
      this.updatePasswordRequirements(value);
    });

    // Redirect if already logged in
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  // Custom validator to check if passwords match
  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
  }

  // Convenience getter for easy access to form fields
  get f() {
    return this.registerForm.controls;
  }

  updatePasswordRequirements(password: string): void {
    if (!password) {
      Object.keys(this.passwordRequirements).forEach(key => {
        this.passwordRequirements[key as keyof typeof this.passwordRequirements] = false;
      });
      return;
    }

    this.passwordRequirements = {
      minLength: password.length >= 8,
      hasUppercase: /[A-Z]/.test(password),
      hasLowercase: /[a-z]/.test(password),
      hasNumber: /[0-9]/.test(password),
      hasSpecialChar: /[@$!%*?&]/.test(password)
    };
  }

  get allPasswordRequirementsMet(): boolean {
    return Object.values(this.passwordRequirements).every(requirement => requirement);
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    // Stop if form is invalid
    if (this.registerForm.invalid) {
      // Show specific validation errors
      if (this.f['password'].errors?.['pattern']) {
        this.errorMessage = 'Password does not meet security requirements. Please check all requirements below.';
      }
      return;
    }

    // Additional password validation check
    if (!this.allPasswordRequirementsMet) {
      this.errorMessage = 'Password does not meet security requirements. Please check all requirements below.';
      return;
    }

    this.loading = true;

    const registerRequest: UserCreateDto = {
      fullName: this.f['fullName'].value,
      email: this.f['email'].value,
      password: this.f['password'].value
    };

    this.authService.register(registerRequest).subscribe({
      next: (response) => {
        console.log('Registration successful', response);
        this.router.navigate(['/auth/verify-email'], {
          queryParams: { email: registerRequest.email }
        });
      },
      error: (error) => {
        console.error('Registration error', error);

        // Handle specific validation errors
        if (error.status === 429) {
          this.errorMessage = 'Too many registration attempts. Please wait a few minutes before trying again.';
        } else if (error.error?.message?.includes('Password must contain')) {
          this.errorMessage = 'Password does not meet security requirements. Please check the requirements below.';
        } else if (error.error?.message?.includes('Email already exists')) {
          this.errorMessage = 'This email is already registered. Please use a different email or try signing in.';
        } else if (error.error?.message?.includes('Username already exists') || error.error?.message?.includes('Full name')) {
          this.errorMessage = 'This username is already taken. Please choose a different one.';
        } else {
          this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
        }

        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}