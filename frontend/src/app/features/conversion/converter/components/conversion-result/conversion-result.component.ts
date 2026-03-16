import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConversionResponseDto } from 'src/app/core/models/conversion.model';

@Component({
  selector: 'app-conversion-result',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="conversion-result" *ngIf="result">
      <div class="result-header">
        <h3>Conversion Result</h3>
        <div class="result-meta">
          <span class="badge">{{ result.inputFormat }} → {{ result.outputFormat }}</span>
          <span class="timestamp">{{ result.createdAt | date:'medium' }}</span>
        </div>
      </div>
      
      <div class="result-content">
        <div class="input-section">
          <h4>Input</h4>
          <div class="code-block">{{ result.prompt }}</div>
        </div>
        
        <div class="output-section">
          <h4>Output</h4>
          <div class="code-block">{{ result.aiResponse }}</div>
          <div class="result-actions">
            <button class="btn btn-primary" (click)="copyToClipboard()">
              <i class="icon">📋</i> Copy Output
            </button>
            <button class="btn btn-secondary" (click)="downloadResult()">
              <i class="icon">💾</i> Download
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .conversion-result {
      background: white;
      border-radius: 0.75rem;
      padding: 1.5rem;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
      margin-top: 2rem;
    }
    
    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
      padding-bottom: 1rem;
      border-bottom: 2px solid #e5e7eb;
    }
    
    .result-header h3 {
      margin: 0;
      color: #1f2937;
    }
    
    .result-meta {
      display: flex;
      gap: 1rem;
      align-items: center;
    }
    
    .badge {
      background: #3b82f6;
      color: white;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    
    .timestamp {
      color: #6b7280;
      font-size: 0.875rem;
    }
    
    .result-content {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 2rem;
    }
    
    .input-section, .output-section {
      background: #f9fafb;
      padding: 1rem;
      border-radius: 0.5rem;
    }
    
    .input-section h4, .output-section h4 {
      margin: 0 0 1rem 0;
      color: #374151;
      font-weight: 600;
    }
    
    .code-block {
      background: #1f2937;
      color: #f9fafb;
      padding: 1rem;
      border-radius: 0.5rem;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 0.875rem;
      white-space: pre-wrap;
      word-break: break-all;
      line-height: 1.5;
    }
    
    .result-actions {
      display: flex;
      gap: 0.75rem;
      margin-top: 1rem;
    }
    
    .icon {
      margin-right: 0.5rem;
    }
    
    @media (max-width: 768px) {
      .result-content {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ConversionResultComponent {
  @Input() result: ConversionResponseDto | null = null;
  
  copyToClipboard(): void {
    if (this.result?.aiResponse) {
      navigator.clipboard.writeText(this.result.aiResponse);
      // Show toast notification
      this.showToast('Output copied to clipboard!');
    }
  }
  
  downloadResult(): void {
    if (this.result) {
      const content = `Input Format: ${this.result.inputFormat}\n\nInput:\n${this.result.prompt}\n\nOutput Format: ${this.result.outputFormat}\n\nOutput:\n${this.result.aiResponse}`;
      const blob = new Blob([content], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `conversion-${this.result.id}.txt`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      
      this.showToast('Conversion downloaded!');
    }
  }
  
  private showToast(message: string): void {
    // Simple toast implementation - could be enhanced with a proper toast service
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.textContent = message;
    toast.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: #10b981;
      color: white;
      padding: 1rem;
      border-radius: 0.5rem;
      z-index: 1000;
      animation: slideIn 0.3s ease-out;
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
      document.body.removeChild(toast);
    }, 3000);
  }
}
