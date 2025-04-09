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


-----------------------

-- First create temporary columns
ALTER TABLE purchase_items
    ADD COLUMN quantity_decimal numeric(12,3) DEFAULT 0.000,
    ADD COLUMN remaining_quantity_decimal numeric(12,3) DEFAULT 0.000;

-- Copy data from old columns to new columns
UPDATE purchase_items
SET quantity_decimal = CAST(quantity AS numeric(12,3)),
    remaining_quantity_decimal = CAST(remaining_quantity AS numeric(12,3));

-- Drop the old columns
ALTER TABLE purchase_items
DROP COLUMN quantity,
DROP COLUMN remaining_quantity;

-- Rename the new columns to the original names
ALTER TABLE purchase_items
    RENAME COLUMN quantity_decimal TO quantity;
ALTER TABLE purchase_items
    RENAME COLUMN remaining_quantity_decimal TO remaining_quantity;

-- Set constraints and defaults
ALTER TABLE purchase_items
    ALTER COLUMN quantity SET NOT NULL,
    ALTER COLUMN quantity SET DEFAULT 0.000,
    ALTER COLUMN remaining_quantity SET DEFAULT 0.000;

----------------------
    -- First create temporary columns
ALTER TABLE product
    ADD COLUMN remaining_quantity_decimal numeric(12,3) DEFAULT 0.000,
    ADD COLUMN blocked_quantity_decimal numeric(12,3) DEFAULT 0.000,
    ADD COLUMN total_remaining_quantity_decimal numeric(12,3) DEFAULT 0.000;

-- Copy data from old columns to new columns
UPDATE product
SET remaining_quantity_decimal = CAST(remaining_quantity AS numeric(12,3)),
    blocked_quantity_decimal = CAST(blocked_quantity AS numeric(12,3)),
    total_remaining_quantity_decimal = CAST(total_remaining_quantity AS numeric(12,3));

-- Drop the old columns
ALTER TABLE product
DROP COLUMN remaining_quantity,
DROP COLUMN blocked_quantity,
DROP COLUMN total_remaining_quantity;

-- Rename the new columns to the original names
ALTER TABLE product
    RENAME COLUMN remaining_quantity_decimal TO remaining_quantity;
ALTER TABLE product
    RENAME COLUMN blocked_quantity_decimal TO blocked_quantity;
ALTER TABLE product
    RENAME COLUMN total_remaining_quantity_decimal TO total_remaining_quantity;

-- Set constraints and defaults
ALTER TABLE product
    ALTER COLUMN remaining_quantity SET DEFAULT 0.000,
    ALTER COLUMN blocked_quantity SET DEFAULT 0.000,
    ALTER COLUMN total_remaining_quantity SET DEFAULT 0.000;