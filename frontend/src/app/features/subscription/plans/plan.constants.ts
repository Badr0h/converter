export const PLANS_DESCRIPTION: Record<string, {
  description: string;
  features: string[];
}> = {
  'Starter': {
    description: 'Perfect for individuals and small projects. Get all the essential AI power to transform your code with ease.',
    features: [
      '300 accurate conversions/mo',
      'All basic & advanced languages',
      'Fast AI processing speed',
      'Standard email support',
      'Secure code handling',
      'Web-based interface access'
    ]
  },
  'Professional': {
    description: 'The sweet spot for developers and power users. Scale your output with massive limits and priority features.',
    features: [
      '1,500 high-speed conversions/mo',
      'Priority AI processing queue',
      'Advanced batch conversion tools',
      'Priority email & chat support',
      'Detailed conversion analytics',
      'Early access to new AI models',
      'Advanced formatting options'
    ]
  },
  'Enterprise': {
    description: 'The ultimate power for teams and large-scale automation. Unlimited flexibility with enterprise-grade security.',
    features: [
      'Unlimited AI conversions',
      'Ultra-fast dedicated processing',
      'Full API & Webhook access',
      'Custom AI model fine-tuning',
      '24/7 Priority VIP support',
      'Dedicated account manager',
      'Custom integrations & SLA',
      'Advanced team management'
    ]
  }
};
