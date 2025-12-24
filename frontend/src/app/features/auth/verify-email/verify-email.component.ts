import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent implements OnInit, OnDestroy {
  verifyForm!: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';
  successMessage = '';
  email = '';
  
  // Resend cooldown
  resendDisabled = false;
  countdown = 60;
  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Get email from query params
    this.email = this.route.snapshot.queryParamMap.get('email') || '';
    
    // Redirect if no email provided
    if (!this.email) {
      this.router.navigate(['/auth/register']);
      return;
    }

    // Initialize form with validation
    this.verifyForm = this.formBuilder.group({
      code: [
        '', 
        [
          Validators.required, 
          Validators.pattern('^[0-9]{6}$'),
          Validators.minLength(6),
          Validators.maxLength(6)
        ]
      ]
    });

    // Auto-focus on code input
    setTimeout(() => {
      const codeInput = document.getElementById('code');
      codeInput?.focus();
    }, 300);

    // Auto-format: only allow numbers
    this.verifyForm.get('code')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        if (value) {
          const numbersOnly = value.replace(/[^0-9]/g, '');
          if (numbersOnly !== value) {
            this.verifyForm.patchValue({ code: numbersOnly }, { emitEvent: false });
          }
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get f() {
    return this.verifyForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Validate form
    if (this.verifyForm.invalid) {
      this.shakeForm();
      return;
    }

    this.loading = true;

    const verifyRequest = {
      email: this.email,
      code: this.f['code'].value.trim()
    };

    this.authService.verifyEmail(verifyRequest).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Email vérifié avec succès! Redirection...';
        this.verifyForm.disable();
        
        // Redirect to login after success
        setTimeout(() => {
          this.router.navigate(['/auth/login'], {
            queryParams: { verified: 'true', email: this.email }
          });
        }, 2000);
      },
      error: (error) => {
        console.error('Verification error:', error);
        
        // Handle specific error messages
        if (error.status === 400) {
          this.errorMessage = 'Code de vérification invalide. Veuillez vérifier et réessayer.';
        } else if (error.status === 410) {
          this.errorMessage = 'Ce code a expiré. Veuillez demander un nouveau code.';
        } else if (error.status === 404) {
          this.errorMessage = 'Email non trouvé. Veuillez vous réinscrire.';
        } else {
          this.errorMessage = error.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
        }
        
        this.loading = false;
        this.shakeForm();
        
        // Clear the code input on error
        this.verifyForm.patchValue({ code: '' });
        document.getElementById('code')?.focus();
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

    if (this.resendDisabled) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.resendVerificationCode(this.email).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Un nouveau code a été envoyé à votre email.';
        this.loading = false;
        this.startResendCooldown();
        
        // Clear success message after 5 seconds
        setTimeout(() => {
          this.successMessage = '';
        }, 5000);
      },
      error: (error) => {
        console.error('Resend error:', error);
        
        if (error.status === 429) {
          this.errorMessage = 'Trop de tentatives. Veuillez patienter avant de réessayer.';
          this.startResendCooldown();
        } else if (error.status === 404) {
          this.errorMessage = 'Email non trouvé. Veuillez vous réinscrire.';
        } else {
          this.errorMessage = error.error?.message || 'Erreur lors de l\'envoi du code. Veuillez réessayer.';
        }
        
        this.loading = false;
      }
    });
  }

  private startResendCooldown(): void {
    this.resendDisabled = true;
    this.countdown = 60;

    interval(1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.countdown--;
        if (this.countdown <= 0) {
          this.resendDisabled = false;
        }
      });

    // Automatically re-enable after 60 seconds
    setTimeout(() => {
      this.resendDisabled = false;
    }, 60000);
  }

  private shakeForm(): void {
    const input = document.getElementById('code');
    input?.classList.add('error');
    setTimeout(() => {
      input?.classList.remove('error');
    }, 400);
  }
}