import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent implements OnInit {
  verifyForm!: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';
  successMessage = '';
  email = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') || '';
    
    this.verifyForm = this.formBuilder.group({
      code: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });
  }

  get f() {
    return this.verifyForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.verifyForm.invalid) {
      return;
    }

    this.loading = true;

    const verifyRequest = {
      email: this.email,
      code: this.f['code'].value
    };

    this.authService.verifyEmail(verifyRequest).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        setTimeout(() => {
          this.router.navigate(['/auth/login']);
        }, 2000);
      },
      error: (error) => {
        console.error('Verification error', error);
        this.errorMessage = error.error?.message || 'Code de vérification invalide. Veuillez réessayer.';
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  resendCode(): void {
    if (!this.email) {
      this.errorMessage = 'Email non trouvé. Veuillez vous réinscrire.';
      return;
    }

    this.loading = true;
    this.authService.resendVerificationCode(this.email).subscribe({
      next: (response) => {
        this.successMessage = 'Un nouveau code de vérification a été envoyé.';
        this.loading = false;
      },
      error: (error) => {
        console.error('Resend error', error);
        this.errorMessage = 'Erreur lors de l\'envoi du code. Veuillez réessayer.';
        this.loading = false;
      }
    });
  }
}
