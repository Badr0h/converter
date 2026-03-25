import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  forgotForm!: FormGroup;
  loading = false;
  submitted = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.forgotForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  get f() {
    return this.forgotForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.forgotForm.invalid) {
      return;
    }

    this.loading = true;

    this.authService.requestPasswordReset(this.f['email'].value).subscribe({
      next: (response) => {
        this.successMessage = 'Un email avec un lien de réinitialisation a été envoyé. Veuillez vérifier votre boîte mail.';
        setTimeout(() => {
          this.router.navigate(['/auth/login']);
        }, 3000);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}
