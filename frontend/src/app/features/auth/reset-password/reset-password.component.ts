import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
  resetForm!: FormGroup;
  loading = false;
  submitted = false;
  successMessage = '';
  errorMessage = '';
  resetToken = '';
  showPassword = false;
  passwordRequirements = {
    minLength: false,
    hasUppercase: false,
    hasLowercase: false,
    hasNumber: false,
    hasSpecialChar: false
  };

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Get reset token from URL query params
    this.resetToken = this.route.snapshot.queryParams['token'];
    
    if (!this.resetToken) {
      this.errorMessage = 'Token de réinitialisation manquant. Veuillez vérifier le lien dans votre email.';
    }

    this.resetForm = this.formBuilder.group({
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });

    // Update password requirements on input
    this.resetForm.get('password')?.valueChanges.subscribe(value => {
      this.updatePasswordRequirements(value);
    });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
  }

  get f() {
    return this.resetForm.controls;
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
    this.successMessage = '';

    if (this.resetForm.invalid) {
      return;
    }

    this.loading = true;

    this.authService.resetPassword(this.resetToken, this.f['password'].value).subscribe({
      next: (response) => {
        this.successMessage = 'Votre mot de passe a été réinitialisé avec succès.';
        setTimeout(() => {
          this.router.navigate(['/auth/login']);
        }, 2000);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Une erreur est survenue lors de la réinitialisation. Veuillez réessayer.';
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}
