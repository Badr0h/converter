import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ConversionService } from 'src/app/core/services/conversion.service';
import { ConversionResponseDto, ConversionCreateDto } from 'src/app/core/models/conversion.model';
import { FormatSelectorComponent } from './components/format-selector/format-selector.component';
import { ConversionInputComponent } from './components/conversion-input/conversion-input.component';
import { ConversionResultComponent } from './components/conversion-result/conversion-result.component';

type FormatLevel = 'recommended' | 'optional' | 'advanced';
type FormatType = 'TEXT' | 'LATEX' | 'MATHML' | 'UNICODE' | 'SYMPY' | 'PYTHON' | 'NUMPY' | 'SCIPY' | 'MATLAB' | 'R' | 'JAVASCRIPT';

interface FormatOption {
  format: string;
  description: string;
  level: FormatLevel;
}

interface ToastMessage {
  message: string;
  type: 'success' | 'error' | 'info';
}

@Component({
  selector: 'app-converter-refactored',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    ReactiveFormsModule,
    FormatSelectorComponent,
    ConversionInputComponent,
    ConversionResultComponent
  ],
  template: `
    <div class="converter-container">
      <div class="converter-header">
        <h2>Mathematical Formula Converter</h2>
        <p class="subtitle">Convert between different mathematical formats</p>
      </div>

      <form *ngIf="conversionForm" [formGroup]="conversionForm" class="conversion-form">
        <div class="form-row">
          <app-conversion-input
            label="Input Formula"
            [value]="conversionForm.get('prompt')?.value || ''"
            [maxLength]="1000"
            [showAdvanced]="showAdvanced"
            (valueChange)="onPromptChange($event)">
          </app-conversion-input>
        </div>

        <div class="form-row" *ngIf="showAdvanced">
          <div class="form-group">
            <app-format-selector
              id="input-format"
              label="Input Format"
              [control]="conversionForm.get('inputFormat')"
              [availableFormats]="inputFormatOptions">
            </app-format-selector>
          </div>
          <div class="form-group">
            <app-format-selector
              id="output-format"
              label="Output Format"
              [control]="conversionForm.get('outputFormat')"
              [availableFormats]="filteredOutputFormats">
            </app-format-selector>
          </div>
        </div>

        <div class="form-actions">
          <button 
            type="button" 
            class="btn btn-outline"
            (click)="toggleAdvanced()">
            {{ showAdvanced ? 'Simple Mode' : 'Advanced Mode' }}
          </button>
          <button 
            type="submit" 
            class="btn btn-primary"
            [disabled]="conversionForm.invalid || loading">
            <span *ngIf="!loading">Convert</span>
            <span *ngIf="loading">
              <i class="spinner"></i> Converting...
            </span>
          </button>
        </div>
      </form>

      <app-conversion-result [result]="conversionResult"></app-conversion-result>

      <div class="toast" *ngIf="toast" [class]="'toast-' + toast.type">
        {{ toast.message }}
      </div>
    </div>
  `,
  styles: [`
    .converter-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem;
    }
    
    .converter-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    
    .converter-header h2 {
      color: #1f2937;
      margin-bottom: 0.5rem;
    }
    
    .subtitle {
      color: #6b7280;
      font-size: 1.125rem;
    }
    
    .conversion-form {
      background: white;
      padding: 2rem;
      border-radius: 0.75rem;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
      margin-bottom: 2rem;
    }
    
    .form-row {
      margin-bottom: 1.5rem;
    }
    
    .form-group {
      margin-bottom: 1rem;
    }
    
    .form-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
    }
    
    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 0.5rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s ease-in-out;
    }
    
    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    
    .btn-primary {
      background: #3b82f6;
      color: white;
    }
    
    .btn-primary:hover:not(:disabled) {
      background: #2563eb;
    }
    
    .btn-outline {
      background: transparent;
      color: #3b82f6;
      border: 2px solid #3b82f6;
    }
    
    .btn-outline:hover {
      background: #3b82f6;
      color: white;
    }
    
    .spinner {
      display: inline-block;
      width: 16px;
      height: 16px;
      border: 2px solid #ffffff;
      border-radius: 50%;
      border-top-color: transparent;
      animation: spin 1s linear infinite;
      margin-right: 0.5rem;
    }
    
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    
    .toast {
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 1rem 1.5rem;
      border-radius: 0.5rem;
      color: white;
      font-weight: 600;
      z-index: 1000;
      animation: slideIn 0.3s ease-out;
    }
    
    .toast-success {
      background: #10b981;
    }
    
    .toast-error {
      background: #ef4444;
    }
    
    .toast-info {
      background: #3b82f6;
    }
    
    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
    
    @media (max-width: 768px) {
      .converter-container {
        padding: 1rem;
      }
      
      .form-actions {
        flex-direction: column;
        gap: 0.75rem;
      }
      
      .btn {
        width: 100%;
      }
    }
  `]
})
export class ConverterRefactoredComponent implements OnInit, OnDestroy {
  conversionForm!: FormGroup;
  loading = false;
  conversionResult: ConversionResponseDto | null = null;
  showAdvanced = false;
  toast: ToastMessage | null = null;
  
  filteredOutputFormats: FormatOption[] = [];
  
  get inputFormatOptions(): FormatOption[] {
    return Object.keys(this.availableFormats).map(f => ({
      format: f,
      description: `Format de source ${f}`,
      level: 'recommended'
    }));
  }
  
  private readonly availableFormats: Record<FormatType, FormatOption[]> = {
    TEXT: [
      { format: 'LATEX', description: 'reports, exams', level: 'recommended' },
      { format: 'UNICODE', description: 'display / chat', level: 'recommended' },
      { format: 'SYMPY', description: 'symbolic computation', level: 'recommended' },
      { format: 'PYTHON', description: 'simple evaluation', level: 'recommended' },
      { format: 'NUMPY', description: 'numerical computation', level: 'optional' },
      { format: 'SCIPY', description: 'scientific computation', level: 'optional' },
      { format: 'MATLAB', description: 'engineering', level: 'advanced' },
      { format: 'R', description: 'statistics', level: 'advanced' },
      { format: 'JAVASCRIPT', description: 'web', level: 'advanced' },
      { format: 'MATHML', description: 'niche / rare', level: 'advanced' },
    ],
    LATEX: [
      { format: 'TEXT', description: 'readable explanation', level: 'recommended' },
      { format: 'UNICODE', description: 'display / chat', level: 'optional' },
      { format: 'SYMPY', description: 'symbolic computation', level: 'recommended' },
      { format: 'PYTHON', description: 'numerical / executable', level: 'recommended' },
      { format: 'NUMPY', description: 'numerical computations', level: 'optional' },
      { format: 'SCIPY', description: 'scientific computations', level: 'optional' },
      { format: 'MATLAB', description: 'engineering code', level: 'advanced' },
      { format: 'R', description: 'statistics', level: 'advanced' },
      { format: 'JAVASCRIPT', description: 'web', level: 'advanced' },
      { format: 'MATHML', description: 'niche / rare', level: 'advanced' },
    ],
    MATHML: [
      { format: 'LATEX', description: 'documentation / reports', level: 'recommended' },
      { format: 'TEXT', description: 'readable explanation', level: 'recommended' },
      { format: 'SYMPY', description: 'symbolic computation', level: 'optional' },
    ],
    UNICODE: [
      { format: 'LATEX', description: 'documentation / reports', level: 'recommended' },
      { format: 'TEXT', description: 'readable explanation', level: 'recommended' },
      { format: 'SYMPY', description: 'symbolic computation', level: 'optional' },
    ],
    SYMPY: [
      { format: 'LATEX', description: 'documentation / reports', level: 'recommended' },
      { format: 'TEXT', description: 'readable explanation', level: 'recommended' },
      { format: 'PYTHON', description: 'numerical / executable', level: 'recommended' },
      { format: 'NUMPY', description: 'numerical computations', level: 'optional' },
      { format: 'SCIPY', description: 'scientific computations', level: 'optional' },
      { format: 'MATLAB', description: 'engineering', level: 'advanced' },
      { format: 'R', description: 'statistics', level: 'advanced' },
      { format: 'JAVASCRIPT', description: 'web', level: 'advanced' },
      { format: 'MATHML', description: 'niche / rare', level: 'advanced' },
    ],
    PYTHON: [
      { format: 'NUMPY', description: 'numerical computations', level: 'recommended' },
      { format: 'SYMPY', description: 'symbolic computation', level: 'recommended' },
      { format: 'SCIPY', description: 'scientific computations', level: 'optional' },
      { format: 'LATEX', description: 'documentation / reports', level: 'optional' },
      { format: 'TEXT', description: 'readable explanation', level: 'optional' },
    ],
    NUMPY: [
      { format: 'PYTHON', description: 'clean executable code', level: 'recommended' },
      { format: 'SYMPY', description: 'symbolic computation', level: 'optional' },
      { format: 'SCIPY', description: 'scientific computation', level: 'optional' },
    ],
    SCIPY: [
      { format: 'LATEX', description: 'documentation / reports', level: 'optional' },
      { format: 'PYTHON', description: 'numerical / executable', level: 'optional' },
      { format: 'NUMPY', description: 'numerical computations', level: 'optional' },
    ],
    MATLAB: [
      { format: 'LATEX', description: 'documentation / reports', level: 'advanced' },
      { format: 'PYTHON', description: 'numerical / executable', level: 'advanced' },
      { format: 'NUMPY', description: 'numerical computations', level: 'advanced' },
    ],
    R: [
      { format: 'LATEX', description: 'documentation / reports', level: 'advanced' },
      { format: 'PYTHON', description: 'numerical / executable', level: 'advanced' },
    ],
    JAVASCRIPT: [
      { format: 'LATEX', description: 'documentation / reports', level: 'advanced' },
      { format: 'PYTHON', description: 'numerical / executable', level: 'advanced' },
    ],
  };
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private conversionService: ConversionService
  ) {}

  ngOnInit(): void {
    this.conversionForm = this.fb.group({
      prompt: ['', [Validators.required]],
      inputFormat: ['TEXT'],
      outputFormat: ['PYTHON']
    });

    // Listen for input format changes to update output format options
    this.conversionForm.get('inputFormat')?.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(inputFormat => {
        this.updateOutputFormats(inputFormat);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onPromptChange(prompt: string): void {
    this.conversionForm.patchValue({ prompt });
  }

  toggleAdvanced(): void {
    this.showAdvanced = !this.showAdvanced;
  }

  private updateOutputFormats(inputFormat: string): void {
    const formatType = inputFormat as FormatType;
    this.filteredOutputFormats = this.availableFormats[formatType] || [];
  }

  onSubmit(): void {
    if (this.conversionForm.invalid) {
      this.showToast('Please fill in all required fields', 'error');
      return;
    }

    this.loading = true;
    this.toast = null;

    const conversionData: ConversionCreateDto = {
      prompt: this.conversionForm.value.prompt!,
      inputFormat: this.conversionForm.value.inputFormat!,
      outputFormat: this.conversionForm.value.outputFormat!
    };

    this.conversionService.createConversion(conversionData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.conversionResult = result;
          this.showToast('Conversion successful!', 'success');
        },
        error: (error) => {
          this.showToast('Conversion failed. Please try again.', 'error');
          console.error('Conversion error:', error);
        },
        complete: () => {
          this.loading = false;
        }
      });
  }

  private showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.toast = { message, type };
    setTimeout(() => {
      this.toast = null;
    }, 3000);
  }
}
