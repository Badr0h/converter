export const PLANS_DESCRIPTION: Record<string, {
  description: string;
  features: string[];
}> = {
  'Basic': {
    description: 'Perfect for individuals and light usage. Get started with basic conversion features to test the platform.',
    features: [
      'Up to 200 conversions per month',
      'Basic file format support',
      'Standard processing speed',
      'Email support'
    ]
  },
  'Pro': {
    description: 'Best for professionals and frequent conversions. Ideal for growing businesses with moderate conversion needs.',
    features: [
      'Up to 500 conversions per month',
      'Extended file format support',
      'Priority processing',
      'Priority email & chat support',
      'Advanced analytics',
      'Batch conversion tools'
    ]
  },
  'Business': {
    description: 'Enterprise-grade solution for large-scale operations. Perfect for teams and organizations with high-volume needs.',
    features: [
      'Up to 1000 conversions per month',
      'All file format support',
      'Ultra-fast processing',
      '24/7 priority support',
      'Advanced analytics & reports',
      'Batch conversion & scheduling',
      'API access',
      'Custom integrations'
    ]
  },
  'Premium': {
    description: 'Unlimited conversions for maximum flexibility. The ultimate package for power users and large enterprises.',
    features: [
      'Unlimited conversions',
      'All file formats supported',
      'Instant processing',
      'Dedicated account manager',
      'Custom analytics & reports',
      'Advanced automation tools',
      'API access with priority support',
      'Custom integrations & webhooks',
      'SLA guarantee'
    ]
  }
};
