import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-conversion-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="conversion-input">
      <label for="prompt-input" class="form-label">
        {{ label }}
      </label>
      <textarea
        id="prompt-input"
        [(ngModel)]="value"
        (ngModelChange)="onValueChange($event)"
        class="form-textarea"
        [placeholder]="placeholder"
        rows="6"
        [maxLength]="maxLength">
      </textarea>
      <div class="input-footer">
        <span class="character-count">
          {{ value?.length || 0 }}/{{ maxLength }}
        </span>
        <span *ngIf="showAdvanced" class="advanced-toggle">
          <button 
            type="button" 
            class="btn btn-outline"
            (click)="toggleAdvanced.emit()">
            {{ showAdvancedOptions ? 'Simple' : 'Advanced' }}
          </button>
        </span>
      </div>
    </div>
  `,
  styles: [`
    .conversion-input {
      margin-bottom: 1.5rem;
    }
    
    .form-label {
      display: block;
      font-weight: 600;
      margin-bottom: 0.5rem;
      color: #374151;
    }
    
    .form-textarea {
      width: 100%;
      padding: 0.75rem;
      border: 2px solid #e5e7eb;
      border-radius: 0.5rem;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 0.875rem;
      resize: vertical;
      transition: border-color 0.15s ease-in-out;
    }
    
    .form-textarea:focus {
      outline: none;
      border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
    
    .input-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 0.5rem;
    }
    
    .character-count {
      font-size: 0.75rem;
      color: #6b7280;
    }
    
    .advanced-toggle .btn {
      padding: 0.25rem 0.75rem;
      font-size: 0.75rem;
    }
  `]
})
export class ConversionInputComponent {
  @Input() label: string = 'Enter your formula';
  @Input() placeholder: string = 'Enter your mathematical formula...';
  @Input() value: string = '';
  @Input() maxLength: number = 1000;
  @Input() showAdvanced: boolean = false;
  
  @Output() valueChange = new EventEmitter<string>();
  @Output() toggleAdvanced = new EventEmitter<void>();
  
  onValueChange(value: string): void {
    this.valueChange.emit(value);
  }
}
