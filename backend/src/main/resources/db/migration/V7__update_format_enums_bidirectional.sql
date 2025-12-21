-- V7__update_format_enums_bidirectional.sql
-- Update format enums to support bidirectional conversion for all formats

-- First, modify the input_format enum to include all formats
ALTER TABLE conversions 
MODIFY COLUMN input_format ENUM('TEXT', 'LATEX', 'MATHML', 'UNICODE', 'PYTHON', 'NUMPY', 'SYMPY', 'SCIPY', 'JAVASCRIPT', 'MATLAB', 'R') NOT NULL DEFAULT 'TEXT';

-- Then, modify the output_format enum to include all formats  
ALTER TABLE conversions 
MODIFY COLUMN output_format ENUM('TEXT', 'LATEX', 'MATHML', 'UNICODE', 'PYTHON', 'NUMPY', 'SYMPY', 'SCIPY', 'JAVASCRIPT', 'MATLAB', 'R') NOT NULL DEFAULT 'PYTHON';

-- Add comment to document the change
ALTER TABLE conversions COMMENT = 'Updated to support bidirectional format conversion - all formats can be used as both input and output';
