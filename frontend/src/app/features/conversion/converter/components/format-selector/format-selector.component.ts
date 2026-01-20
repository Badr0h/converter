import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

type FormatLevel = 'recommended' | 'optional' | 'advanced';
type FormatType = 'TEXT' | 'LATEX' | 'MATHML' | 'UNICODE' | 'SYMPY' | 'PYTHON' | 'NUMPY' | 'SCIPY' | 'MATLAB' | 'R' | 'JAVASCRIPT';

interface FormatOption {
  format: string;
  description: string;
  level: FormatLevel;
}

@Component({
  selector: 'app-format-selector',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="format-selector">
      <label [for]="id" class="form-label">{{ label }}</label>
      <select 
        [id]="id"
        [formControl]="control"
        class="form-select"
        (change)="onSelectionChange($event)">
        <option value="">Select a format...</option>
        <option 
          *ngFor="let option of availableFormats" 
          [value]="option.format"
          [class]="'format-option ' + option.level">
          {{ option.format }} - {{ option.description }}
        </option>
      </select>
    </div>
  `,
  styles: [`
    .format-selector {
      margin-bottom: 1rem;
    }
    
    .form-label {
      display: block;
      font-weight: 600;
      margin-bottom: 0.5rem;
      color: #374151;
    }
    
    .form-select {
      width: 100%;
      padding: 0.75rem;
      border: 2px solid #e5e7eb;
      border-radius: 0.5rem;
      background-color: white;
      font-size: 0.875rem;
      transition: border-color 0.15s ease-in-out;
    }
    
    .form-select:focus {
      outline: none;
      border-color: #3b82f6;
      box-shadow: 0 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
    
    .format-option.recommended {
      font-weight: 600;
      color: #059669;
    }
    
    .format-option.optional {
      color: #6b7280;
    }
    
    .format-option.advanced {
      color: #9ca3af;
      font-style: italic;
    }
  `]
})
export class FormatSelectorComponent {
  @Input() id: string = '';
  @Input() label: string = 'Format';
  @Input() control: any;
  @Input() availableFormats: FormatOption[] = [];
  
  @Output() selectionChange = new EventEmitter<string>();
  
  onSelectionChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.selectionChange.emit(target.value);
  }
}
