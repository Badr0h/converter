export enum InputFormat {
  TEXT = 'TEXT',
  LATEX = 'LATEX',
  MATHML = 'MATHML',
  UNICODE = 'UNICODE'
}

export enum OutputFormat {
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
  outputFormat: OutputFormat;
  inputFormat: InputFormat;
  aiResponse: string;
  prompt: string;
  createdAt: Date;
}

export interface ConversionCreateDto {
  outputFormat: OutputFormat;
  inputFormat: InputFormat;
  prompt: string;
}