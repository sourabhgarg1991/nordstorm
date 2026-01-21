resource "aws_s3_bucket" "ertm_transaction_source_bucket" {
  count  = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  bucket = "${var.app_id}-ertm-transaction-source-${local.environment}"
}

resource "aws_s3_bucket" "ertm_transaction_processed_bucket" {
  count  = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  bucket = "${var.app_id}-ertm-transaction-processed-${local.environment}"
}

resource "aws_s3_bucket" "ertm_query_source_bucket" {
  count  = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  bucket = "${var.app_id}-ertm-query-source-${local.environment}"
}

resource "aws_s3_bucket" "data_integration_test_bucket" {
  count  = local.is_nonprod ? 1 : 0 # created only in nonprod
  bucket = "${var.app_id}-data-integration-test"
}

resource "aws_s3_bucket_lifecycle_configuration" "test-bucket-config" {
  count  = local.is_nonprod ? 1 : 0 # created only in nonprod
  bucket = aws_s3_bucket.data_integration_test_bucket[count.index].id
  rule {
    id = "DataExpirationRule"
    expiration {
      days = 1
    }
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "bucket-config" {
  count  = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  bucket = aws_s3_bucket.ertm_transaction_source_bucket[count.index].id
  rule {
    id = "DataExpirationRule"
    expiration {
      days = local.ertm_data_expiration_in_days[local.environment]
    }
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "processed_bucket_config" {
  count  = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  bucket = aws_s3_bucket.ertm_transaction_processed_bucket[count.index].id
  rule {
    id = "DataExpirationRule"
    expiration {
      days = local.ertm_data_expiration_in_days[local.environment]
    }
    status = "Enabled"
  }
}

resource "aws_s3_object" "sql_file_upload" {
  count        = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  bucket       = aws_s3_bucket.ertm_query_source_bucket[count.index].id
  key          = "ertm_aedw_transaction_data_extract.sql"
  source       = local.ertm_query_source_file
  content_type = "text/plain"
  etag         = md5(timestamp())
}

data "aws_iam_policy_document" "allow_teradata_ec2_role_to_read_s3_buckets" {
  count = local.is_nonprod_or_prod ? 1 : 0
  statement {
    sid    = "AllowListBucket"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.teradata_ec2_instance_access_role]
    }
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.ertm_query_source_bucket[count.index].arn]
  }

  statement {
    sid    = "AllowReadObjects"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.teradata_ec2_instance_access_role]
    }
    actions = [
      "s3:GetObject",
      "s3:GetObjectVersion"
    ]
    resources = ["${aws_s3_bucket.ertm_query_source_bucket[count.index].arn}/*"]
  }
}

data "aws_iam_policy_document" "allow_red_shift_services_to_put_to_s3_buckets" {
  count = local.is_nonprod_or_prod ? 1 : 0
  statement {
    sid    = "AllowListAndPutObjects"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.red_shift_services_access_role]
    }
    actions = [
      "s3:PutObject*",
      "s3:ListBucket"
    ]
    resources = [
      "${aws_s3_bucket.ertm_transaction_source_bucket[count.index].arn}/*",
      "${aws_s3_bucket.ertm_transaction_source_bucket[count.index].arn}"
    ]
  }
}

resource "aws_s3_bucket_policy" "ertm_query_source_bucket_policy" {
  count  = local.is_nonprod_or_prod ? 1 : 0
  bucket = aws_s3_bucket.ertm_query_source_bucket[count.index].id
  policy = data.aws_iam_policy_document.allow_teradata_ec2_role_to_read_s3_buckets[count.index].json
}

resource "aws_s3_bucket_policy" "ertm_transaction_source_bucket_policy" {
  count  = local.is_nonprod_or_prod ? 1 : 0
  bucket = aws_s3_bucket.ertm_transaction_source_bucket[count.index].id
  policy = data.aws_iam_policy_document.allow_red_shift_services_to_put_to_s3_buckets[count.index].json
}