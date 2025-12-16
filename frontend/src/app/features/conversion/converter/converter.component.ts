import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ConversionService } from '../../../core/services/conversion.service';
import { ConversionResponseDto, ConversionCreateDto } from '../../../core/models/conversion.model';

@Component({
  selector: 'app-converter',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './converter.component.html',
  styleUrl: './converter.component.scss'
})
export class ConverterComponent implements OnInit {
  conversionForm!: FormGroup;
  loading = false;
  conversionResult: ConversionResponseDto | null = null;
  errorMessage = '';
  
  // Available formats - you can expand this based on your needs
  availableFormats = [
    'TEXT',
    'LATEX',
    'MATHML',
    'UNICODE',
    'PYTHON',
    'NUMPY',
    'SYMPY',
    'SCIPY',
    'JAVASCRIPT',
    'MATLAB',
    'R'
  ];

  // History of conversions in this session
  conversionHistory: ConversionResponseDto[] = [];
  showHistory = false;

  constructor(
    private formBuilder: FormBuilder,
    private conversionService: ConversionService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadHistory();
    this.checkForViewParam();
  }

  initForm(): void {
    this.conversionForm = this.formBuilder.group({
      inputFormat: ['JSON', Validators.required],
      outputFormat: ['XML', Validators.required],
      prompt: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  loadHistory(): void {
    this.conversionService.getConversionHistory().subscribe({
      next: (conversions) => {
        this.conversionHistory = conversions
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      },
      error: (error) => {
        console.error('Error loading history', error);
      }
    });
  }

  checkForViewParam(): void {
    // Check if we're viewing a specific conversion
    this.route.queryParams.subscribe(params => {
      const viewId = params['view'];
      if (viewId) {
        this.loadConversionById(Number(viewId));
      }
    });
  }

  loadConversionById(id: number): void {
    this.conversionService.getConversionById(id).subscribe({
      next: (conversion) => {
        this.conversionResult = conversion;
        // Populate form with the loaded conversion
        this.conversionForm.patchValue({
          inputFormat: conversion.inputFormat,
          outputFormat: conversion.outputFormat,
          prompt: conversion.prompt
        });
      },
      error: (error) => {
        console.error('Error loading conversion', error);
        this.errorMessage = 'Failed to load conversion';
      }
    });
  }

  get f() {
    return this.conversionForm.controls;
  }

  onSubmit(): void {
    if (this.conversionForm.invalid) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.conversionResult = null;

    const request: ConversionCreateDto = {
      inputFormat: this.f['inputFormat'].value,
      outputFormat: this.f['outputFormat'].value,
      prompt: this.f['prompt'].value
    };

    this.conversionService.createConversion(request).subscribe({
      next: (response) => {
        console.log('Conversion successful', response);
        this.conversionResult = response;
        this.loading = false;
        // Add to history
        this.conversionHistory.unshift(response);
      },
      error: (error) => {
        console.error('Conversion error', error);
        this.errorMessage = error.error?.message || 'Conversion failed. Please try again.';
        this.loading = false;
      }
    });
  }

  swapFormats(): void {
    const inputFormat = this.f['inputFormat'].value;
    const outputFormat = this.f['outputFormat'].value;
    
    this.conversionForm.patchValue({
      inputFormat: outputFormat,
      outputFormat: inputFormat
    });
  }

  clearForm(): void {
    this.conversionForm.reset({
      inputFormat: 'JSON',
      outputFormat: 'XML',
      prompt: ''
    });
    this.conversionResult = null;
    this.errorMessage = '';
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      alert('Copied to clipboard! ✓');
    }).catch(err => {
      console.error('Failed to copy:', err);
      alert('Failed to copy to clipboard');
    });
  }

  copyInput(): void {
    this.copyToClipboard(this.f['prompt'].value);
  }

  copyOutput(): void {
    if (this.conversionResult?.aiResponse) {
      this.copyToClipboard(this.conversionResult.aiResponse);
    }
  }

  loadFromHistory(conversion: ConversionResponseDto): void {
    this.conversionForm.patchValue({
      inputFormat: conversion.inputFormat,
      outputFormat: conversion.outputFormat,
      prompt: conversion.prompt
    });
    this.conversionResult = conversion;
    this.showHistory = false;
  }

  deleteFromHistory(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('Delete this conversion from history?')) {
      this.conversionService.deleteConversion(id).subscribe({
        next: () => {
          this.conversionHistory = this.conversionHistory.filter(c => c.id !== id);
          if (this.conversionResult?.id === id) {
            this.conversionResult = null;
          }
        },
        error: (error) => {
          console.error('Error deleting conversion', error);
          alert('Failed to delete conversion');
        }
      });
    }
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  useAsInput(): void {
    if (this.conversionResult?.aiResponse) {
      this.conversionForm.patchValue({
        prompt: this.conversionResult.aiResponse,
        inputFormat: this.conversionResult.outputFormat
      });
      this.conversionResult = null;
    }
  }
}