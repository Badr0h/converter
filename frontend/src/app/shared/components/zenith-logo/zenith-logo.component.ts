import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-zenith-logo',
  standalone: true,
  template: `
    <svg
      [attr.width]="size"
      [attr.height]="size"
      viewBox="0 0 120 120"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      [class]="'zenith-logo ' + variant"
    >
      <!-- Background Circle (optional) -->
      <circle *ngIf="showBackground" cx="60" cy="60" r="58" fill="currentColor" opacity="0.05" stroke="currentColor" stroke-width="1" opacity="0.1"/>

      <!-- Main Conversion Arrow/Zenith Symbol -->
      <!-- Upward arrow representing 'Zenith' (peak) -->
      <path
        d="M 60 15 L 100 75 L 85 75 L 85 100 L 35 100 L 35 75 L 20 75 Z"
        fill="currentColor"
        opacity="0.9"
      />

      <!-- Accent geometric shape - represents conversion/transformation -->
      <path
        d="M 50 50 L 70 50 L 60 65 Z"
        fill="white"
        opacity="0.8"
      />

      <!-- Subtle gradient line suggesting movement -->
      <line x1="40" y1="40" x2="80" y2="80" stroke="currentColor" stroke-width="2" opacity="0.3"/>
    </svg>
  `,
  styles: [`
    :host {
      display: inline-block;
    }

    .zenith-logo {
      transition: transform 0.2s ease-in-out;

      &.brand {
        color: var(--primary-600);
      }

      &.white {
        color: white;
      }

      &.dark {
        color: var(--gray-900);
      }

      &:hover {
        transform: scale(1.05);
      }
    }
  `]
})
export class ZenithLogoComponent {
  @Input() size: string = '40';
  @Input() variant: 'brand' | 'white' | 'dark' = 'brand';
  @Input() showBackground: boolean = false;
}
