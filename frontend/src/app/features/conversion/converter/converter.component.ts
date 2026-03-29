import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ConversionService } from '../../../core/services/conversion.service';
import { ConversionResponseDto, ConversionCreateDto, UsageStatsDto } from '../../../core/models/conversion.model';

// Type definitions
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
  selector: 'app-converter',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './converter.component.html',
  styleUrls: ['./converter.component.scss']
})
export class ConverterComponent implements OnInit, OnDestroy {
  // Form and state
  conversionForm!: FormGroup;
  loading = false;
  conversionResult: ConversionResponseDto | null = null;
  errorMessage = '';
  showAdvanced = false;
  showHistory = false;

  // Usage statistics
  usageStats: UsageStatsDto | null = null;
  loadingUsageStats = false;
  usageStatsError = '';

  // Toast notification
  toast: ToastMessage | null = null;

  // Filtered output formats
  filteredOutputFormats: FormatOption[] = [];

  // Available formats with proper typing
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
      { format: 'TEXT', description: 'readable explanation', level: 'optional' },
      { format: 'LATEX', description: 'documentation / reports', level: 'optional' },
    ],
    SCIPY: [
      { format: 'PYTHON', description: 'clean executable code', level: 'recommended' },
      { format: 'NUMPY', description: 'numerical computations', level: 'recommended' },
      { format: 'TEXT', description: 'readable explanation', level: 'optional' },
    ],
    MATLAB: [
      { format: 'PYTHON', description: 'numerical / executable', level: 'recommended' },
      { format: 'NUMPY', description: 'numerical computations', level: 'recommended' },
      { format: 'SCIPY', description: 'scientific computation', level: 'optional' },
      { format: 'TEXT', description: 'readable explanation', level: 'optional' },
    ],
    R: [
      { format: 'PYTHON', description: 'numerical / executable', level: 'recommended' },
      { format: 'NUMPY', description: 'numerical computations', level: 'recommended' },
      { format: 'TEXT', description: 'readable explanation', level: 'optional' },
    ],
    JAVASCRIPT: [
      { format: 'PYTHON', description: 'numerical / executable', level: 'recommended' },
      { format: 'NUMPY', description: 'numerical computations', level: 'optional' },
      { format: 'TEXT', description: 'readable explanation', level: 'optional' },
    ],
  };

  // History
  conversionHistory: ConversionResponseDto[] = [];

  // Destroy subject for cleanup
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly conversionService: ConversionService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.initForm();
    this.loadHistory();
    this.loadUsageStats();
    this.checkForViewParam();
    this.setupFormSubscriptions();

    // Initialize filtered outputs
    const initialInput = this.conversionForm.get('inputFormat')?.value;
    if (initialInput) {
      this.updateOutputFormats(initialInput);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.conversionForm = this.formBuilder.group({
      inputFormat: ['TEXT', Validators.required],
      outputFormat: ['PYTHON', Validators.required],
      prompt: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  private setupFormSubscriptions(): void {
    // Watch input format changes with debounce to prevent excessive updates
    this.conversionForm.get('inputFormat')?.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(100),
        distinctUntilChanged()
      )
      .subscribe(inputFormat => {
        if (inputFormat) {
          this.updateOutputFormats(inputFormat);
        }
      });
  }

  private updateOutputFormats(inputFormat: string): void {
    const formats = this.availableFormats[inputFormat as FormatType];

    if (!formats) {
      console.warn(`No formats available for ${inputFormat}`);
      this.filteredOutputFormats = [];
      this.cdr.detectChanges();
      return;
    }

    // Filter by advanced mode
    this.filteredOutputFormats = this.showAdvanced
      ? [...formats]
      : formats.filter(f => f.level !== 'advanced');

    console.log('[converter] updated filteredOutputFormats:', this.filteredOutputFormats.map(f => f.format));

    // Set default output format intelligently
    this.setDefaultOutputFormat();

    this.cdr.detectChanges();
  }

  private setDefaultOutputFormat(): void {
    if (this.filteredOutputFormats.length === 0) return;

    const currentOutput = this.conversionForm.get('outputFormat')?.value;
    const isCurrentValid = this.filteredOutputFormats.some(f => f.format === currentOutput);

    // Only change if current selection is invalid
    if (!isCurrentValid) {
      const recommended = this.filteredOutputFormats.find(f => f.level === 'recommended');
      const defaultFormat = recommended?.format ?? this.filteredOutputFormats[0].format;

      this.conversionForm.patchValue(
        { outputFormat: defaultFormat },
        { emitEvent: false } // Prevent circular updates
      );
    }
  }

  toggleAdvancedMode(): void {
    console.log('[converter] BUTTON CLICKED');
    this.showAdvanced = !this.showAdvanced;
    console.log('[converter] showAdvanced is now:', this.showAdvanced);
    const inputFormat = this.conversionForm.get('inputFormat')?.value;
    if (inputFormat) {
      console.log('[converter] updating output formats for:', inputFormat);
      this.updateOutputFormats(inputFormat);
      console.log('[converter] filteredOutputFormats:', this.filteredOutputFormats.map(f => f.format));
    }
  }

  private loadHistory(): void {
    this.conversionService.getConversionHistory()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (conversions) => {
          this.conversionHistory = conversions
            .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        },
        error: (error) => {
          console.error('Error loading history', error);
          this.showToast('Failed to load conversion history', 'error');
        }
      });
  }

  private loadUsageStats(): void {
    this.loadingUsageStats = true;
    this.usageStatsError = '';
    this.conversionService.getUserUsageStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats) => {
          this.usageStats = stats;
          this.loadingUsageStats = false;
        },
        error: (error) => {
          console.error('Error loading usage stats', error);
          this.usageStatsError = error.error?.message || 'Failed to load usage statistics';
          this.loadingUsageStats = false;
          // Don't show toast on error - it's not critical
        }
      });
  }

  private checkForViewParam(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const viewId = params['view'];
        if (viewId) {
          this.loadConversionById(Number(viewId));
        }
      });
  }

  private loadConversionById(id: number): void {
    if (isNaN(id)) {
      this.showToast('Invalid conversion ID', 'error');
      return;
    }

    this.conversionService.getConversionById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (conversion) => {
          this.conversionResult = conversion;
          this.conversionForm.patchValue({
            inputFormat: conversion.inputFormat,
            outputFormat: conversion.outputFormat,
            prompt: conversion.prompt
          });
        },
        error: (error) => {
          console.error('Error loading conversion', error);
          this.errorMessage = 'Failed to load conversion';
          this.showToast('Failed to load conversion', 'error');
        }
      });
  }

  get f() {
    return this.conversionForm.controls;
  }

  get isSubmitDisabled(): boolean {
    return this.conversionForm.invalid 
      || this.loading 
      || this.loadingUsageStats
      || this.usageStats?.isDailyLimitExceeded === true
      || this.usageStats?.isMonthlyLimitExceeded === true;
  }

  get usageWarningMsg(): string {
    if (!this.usageStats) return '';
    
    if (this.usageStats.isDailyLimitExceeded) {
      return '⛔ Daily limit reached';
    }
    if (this.usageStats.isMonthlyLimitExceeded) {
      return '⛔ Monthly limit reached';
    }
    if (this.usageStats.dailyPercentage >= 80) {
      return `⚠️ 80% of daily limit (${this.usageStats.dailyUsage}/${this.usageStats.dailyLimit})`;
    }
    if (this.usageStats.monthlyPercentage >= 80) {
      return `⚠️ 80% of monthly limit (${this.usageStats.monthlyUsage}/${this.usageStats.monthlyLimit})`;
    }
    return '';
  }

  onSubmit(): void {
    // Check if daily or monthly limits are exceeded
    if (this.usageStats?.isDailyLimitExceeded) {
      this.errorMessage = 'Daily conversion limit exceeded. Please upgrade your plan or try again tomorrow.';
      this.showToast(this.errorMessage, 'error');
      return;
    }

    if (this.usageStats?.isMonthlyLimitExceeded) {
      this.errorMessage = 'Monthly conversion limit exceeded. Please upgrade your plan or try again next month.';
      this.showToast(this.errorMessage, 'error');
      return;
    }

    // Mark all fields as touched to show validation errors
    if (this.conversionForm.invalid) {
      Object.keys(this.conversionForm.controls).forEach(key => {
        this.conversionForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.conversionResult = null;

    const inputFormat = this.f['inputFormat'].value;
    const outputFormat = this.f['outputFormat'].value;
    const userPrompt = this.f['prompt'].value;

    // Structure the prompt for better AI response
    const structuredPrompt = this.buildStructuredPrompt(inputFormat, outputFormat, userPrompt);

    const request: ConversionCreateDto = {
      inputFormat: inputFormat,
      outputFormat: outputFormat,
      prompt: structuredPrompt
    };

    this.conversionService.createConversion(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.conversionResult = response;
          this.loading = false;
          this.conversionHistory.unshift(response);
          this.showToast('Conversion successful!', 'success');
          // Reload usage stats after successful conversion
          this.loadUsageStats();
        },
        error: (error) => {
          this.loading = false;
          const errorMessage = error.error?.message || 'Conversion failed. Please try again.';
          
          // Handle specific limit exceeded errors
          if (errorMessage.includes('Daily limit exceeded') || errorMessage.includes('limit exceeded')) {
            this.errorMessage = `⚠️ ${errorMessage}. Please upgrade your plan or try again later.`;
          } else if (errorMessage.includes('no subscription')) {
            this.errorMessage = '📋 No active subscription found. Please subscribe to start converting.';
          } else {
            this.errorMessage = errorMessage;
          }
          
          this.showToast(this.errorMessage, 'error');
          // Reload usage stats after error to get updated limits
          this.loadUsageStats();
        }
      });
  }

  /**
   * Builds a structured prompt for better AI conversion results
   */
  private buildStructuredPrompt(inputFormat: string, outputFormat: string, userPrompt: string): string {
    return `You are a code conversion tool. Your ONLY task is to convert the input code from ${inputFormat} to ${outputFormat}.

      RULES:
      - RETURN ONLY the converted code
      - NO explanations, comments, or markdown
      - NO backticks or code blocks
      - NO "Here's the code" or similar phrases
      - NO additional text before or after
      - PRESERVE all logic, operations, and functionality exactly
      - USE ${outputFormat} syntax and conventions

      Input (${inputFormat}):
      ${userPrompt.trim()}

      Converted ${outputFormat} code (ONLY the code, no other text):`;
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
      inputFormat: 'TEXT',
      outputFormat: 'PYTHON',
      prompt: ''
    });
    this.conversionResult = null;
    this.errorMessage = '';
  }

  async copyToClipboard(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.showToast('Copied to clipboard!', 'success');
    } catch (error) {
      console.error('Failed to copy to clipboard', error);
      this.showToast('Failed to copy to clipboard', 'error');
    }
  }

  copyInput(): void {
    const prompt = this.f['prompt'].value;
    if (prompt) {
      this.copyToClipboard(prompt);
    }
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

    if (!this.confirmAction('Delete this conversion from history?')) {
      return;
    }

    this.conversionService.deleteConversion(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.conversionHistory = this.conversionHistory.filter(c => c.id !== id);
          if (this.conversionResult?.id === id) {
            this.conversionResult = null;
          }
          this.showToast('Conversion deleted', 'success');
        },
        error: (error) => {
          console.error('Failed to delete conversion', error);
          this.showToast('Failed to delete conversion', 'error');
        }
      });
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
  }

  formatDate(date: Date | string): string {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    return dateObj.toLocaleDateString('en-US', {
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
      this.showToast('Output loaded as new input', 'info');
    }
  }

  // Toast notification system (replace alert/confirm)
  private showToast(message: string, type: ToastMessage['type']): void {
    this.toast = { message, type };
    setTimeout(() => {
      this.toast = null;
    }, 3000);
  }

  // Better confirmation dialog (can be replaced with a modal component)
  private confirmAction(message: string): boolean {
    return confirm(message);
  }

  // Helper to check if a format is valid
  isValidFormat(format: string): boolean {
    return format in this.availableFormats;
  }

  // Get all available input formats
  get availableInputFormats(): FormatType[] {
    return Object.keys(this.availableFormats) as FormatType[];
  }

  // TrackBy function for performance optimization in *ngFor
  trackByConversionId(index: number, conversion: ConversionResponseDto): number {
    return conversion.id;
  }
}