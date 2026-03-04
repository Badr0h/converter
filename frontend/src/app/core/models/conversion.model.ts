export enum Format {
  TEXT = 'TEXT',
  LATEX = 'LATEX',
  MATHML = 'MATHML',
  UNICODE = 'UNICODE',
  PYTHON = 'PYTHON',
  NUMPY = 'NUMPY',
  SYMPY = 'SYMPY',
  SCIPY = 'SCIPY',
  JAVASCRIPT = 'JAVASCRIPT',
  MATLAB = 'MATLAB',
  R = 'R'
}

export interface ConversionResponseDto {
  id: number;
  userId?: number;
  userEmail?: string;
  outputFormat: Format;
  inputFormat: Format;
  aiResponse: string;
  prompt: string;
  createdAt: Date;
}

export interface ConversionCreateDto {
  outputFormat: Format;
  inputFormat: Format;
  prompt: string;
}