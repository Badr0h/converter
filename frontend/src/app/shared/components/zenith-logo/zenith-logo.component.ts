import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-zenith-logo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './zenith-logo.component.html',
  styleUrl: './zenith-logo.component.scss'
})
export class ZenithLogoComponent implements OnInit {
  /**
   * Size of the logo (width and height)
   * Examples: '24px', '32px', '48px', '64px'
   */
  @Input() size: string = '32px';

  /**
   * Color of the logo
   * Default: Indigo vibrant (#7C3AED)
   */
  @Input() color: string = '#7C3AED';

  /**
   * Optional title for accessibility
   */
  @Input() title: string = 'ZenithConvert Logo';

  // Dynamic styles binding
  logoStyles: any = {};

  ngOnInit() {
    this.logoStyles = {
      width: this.size,
      height: this.size,
      color: this.color
    };
  }
}
