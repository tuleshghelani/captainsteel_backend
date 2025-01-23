-- First create a temporary column
ALTER TABLE quotation_items
    ADD COLUMN quantity_decimal numeric(12,3) DEFAULT 0.000;

-- Copy data from old column to new column
UPDATE quotation_items
SET quantity_decimal = CAST(quantity AS numeric(12,3));

-- Drop the old column
ALTER TABLE quotation_items
DROP COLUMN quantity;

-- Rename the new column to the original name
ALTER TABLE quotation_items
    RENAME COLUMN quantity_decimal TO quantity;

-- Set constraints and default
ALTER TABLE quotation_items
    ALTER COLUMN quantity SET NOT NULL,
ALTER COLUMN quantity SET DEFAULT 0.000;