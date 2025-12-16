import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  
  features = [
    {
      icon: '🔄',
      title: 'AI-Powered Conversion',
      description: 'Convert between any text format using advanced AI technology. From JSON to XML, Python to Java, and more.'
    },
    {
      icon: '⚡',
      title: 'Lightning Fast',
      description: 'Get instant conversions powered by state-of-the-art language models. No waiting, no hassle.'
    },
    {
      icon: '🎯',
      title: 'High Accuracy',
      description: 'Our AI understands context and structure to provide accurate, reliable conversions every time.'
    },
    {
      icon: '🔒',
      title: 'Secure & Private',
      description: 'Your data is encrypted and never stored. Complete privacy and security guaranteed.'
    },
    {
      icon: '📚',
      title: 'Multiple Formats',
      description: 'Support for programming languages, data formats, markup languages, and more.'
    },
    {
      icon: '💎',
      title: 'Premium Features',
      description: 'Unlimited conversions, API access, priority support, and advanced AI models.'
    }
  ];

  supportedFormats = [
    { name: 'JSON', symbol: '{ }', color: '#f7df1e' },
    { name: 'XML', symbol: '< >', color: '#e44d26' },
    { name: 'Python', symbol: 'py', color: '#3776ab' },
    { name: 'Java', symbol: 'J', color: '#007396' },
    { name: 'JavaScript', symbol: 'JS', color: '#f7df1e' },
    { name: 'TypeScript', symbol: 'TS', color: '#3178c6' },
    { name: 'SQL', symbol: '⚡', color: '#00758f' },
    { name: 'YAML', symbol: 'YML', color: '#cb171e' },
    { name: 'CSV', symbol: '📊', color: '#217346' },
    { name: 'HTML', symbol: '<h>', color: '#e44d26' },
    { name: 'Markdown', symbol: 'MD', color: '#000000' },
    { name: 'C++', symbol: 'C++', color: '#00599c' }
  ];

  mathSymbols = ['∫', '∑', '∏', '√', '∞', 'π', 'Δ', 'Ω', 'λ', '∂', '∇', 'α', 'β', 'γ'];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  scrollToFeatures(): void {
    const element = document.getElementById('features');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}