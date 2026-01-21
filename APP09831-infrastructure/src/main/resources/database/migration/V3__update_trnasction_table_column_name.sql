DROP INDEX IF EXISTS idx_transaction_line_of_business;

ALTER TABLE "transaction" RENAME COLUMN line_of_business TO source_reference_type;

CREATE index if NOT EXISTS idx_transaction_source_reference_type
    ON "transaction" ("source_reference_type");