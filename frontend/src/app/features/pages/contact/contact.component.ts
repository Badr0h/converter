import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ContactService } from '../../../core/services/api/pages/contact.service';

@Component({
    selector: 'app-contact',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
    templateUrl: './contact.component.html',
    styleUrl: './contact.component.scss'
})
export class ContactComponent {
    contactForm: FormGroup;
    isSubmitting = false;
    isSuccess = false;
    errorMessage: string | null = null;

    constructor(
        private fb: FormBuilder,
        private contactService: ContactService
    ) {
        this.contactForm = this.fb.group({
            name: ['', [Validators.required, Validators.minLength(2)]],
            email: ['', [Validators.required, Validators.email]],
            subject: ['', [Validators.required]],
            message: ['', [Validators.required, Validators.minLength(10)]]
        });
    }

    get currentYear(): number {
        return new Date().getFullYear();
    }

    onSubmit() {
        if (this.contactForm.valid) {
            this.isSubmitting = true;
            this.errorMessage = null;

            this.contactService.submitMessage(this.contactForm.value).subscribe({
                next: (response) => {
                    console.log('Form submitted successfully:', response);
                    this.isSubmitting = false;
                    this.isSuccess = true;
                    this.contactForm.reset();

                    // Reset success message after 5 seconds
                    setTimeout(() => this.isSuccess = false, 5000);
                },
                error: (error) => {
                    console.error('Error submitting form:', error);
                    this.isSubmitting = false;
                    this.errorMessage = "Une erreur est survenue lors de l'envoi de votre message. Veuillez réessayer.";
                }
            });
        } else {
            this.markFormGroupTouched(this.contactForm);
        }
    }

    private markFormGroupTouched(formGroup: FormGroup) {
        Object.values(formGroup.controls).forEach(control => {
            control.markAsTouched();
            if ((control as any).controls) {
                this.markFormGroupTouched(control as FormGroup);
            }
        });
    }
}
