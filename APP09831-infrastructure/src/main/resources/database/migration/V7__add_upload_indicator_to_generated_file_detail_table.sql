ALTER TABLE "generated_file_detail" ADD COLUMN IF NOT EXISTS is_uploaded_to_s3 boolean NOT NULL DEFAULT false;

update generated_file_detail set is_uploaded_to_s3 = true where is_uploaded_to_s3 IS NULL;