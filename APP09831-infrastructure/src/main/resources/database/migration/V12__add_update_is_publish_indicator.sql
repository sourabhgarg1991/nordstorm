ALTER TABLE "transaction_aggregation_relation" RENAME COLUMN is_published TO is_published_to_data_platform;
ALTER TABLE "filtered_transaction" RENAME COLUMN is_published TO is_published_to_data_platform;
ALTER TABLE "generated_file_detail" ADD COLUMN IF NOT EXISTS is_published_to_data_platform boolean NOT NULL DEFAULT false;
