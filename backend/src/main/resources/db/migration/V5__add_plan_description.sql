-- V5__add_plan_description.sql
-- Add description column to plans table

ALTER TABLE plans 
ADD COLUMN description TEXT AFTER name;
