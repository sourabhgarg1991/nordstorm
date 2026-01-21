locals {
  is_nonprod_or_prod                        = "prod" == lower(var.environment) || "nonprod" == lower(var.environment)
  is_prod                                   = "prod" == lower(var.environment)
  is_nonprod                                = "nonprod" == lower(var.environment)
  environment                               = "prod" != lower(var.environment) && "nonprod" != lower(var.environment) ? "dev" : var.environment
  default_region                            = "us-west-2"
  default_region_availability_zones         = ["us-west-2a", "us-west-2b", "us-west-2c"]
  aurora_postgresql_name_prefix             = "data-integration"
  aurora_postgresql_port                    = "5432"
  aurora_postgresql_family                  = "aurora-postgresql16"
  aurora_postgresql_engine                  = "aurora-postgresql"
  aurora_postgresql_engine_version          = "16.6"
  auto_minor_version_upgrade                = true
  iam_aurora_postgresql_auth_enabled        = true
  aurora_postgresql_storage_encrypted       = true
  aurora_postgresql_apply_immediately       = false # Setting as false to defer database modifications until the next maintenance window in order to avoid immediate downtime.  To apply a database modification manually (without waiting for maintenance window), login to AWS CLI and run the below command: `aws rds modify-db-instance --db-instance-identifier <instance-name> --region <region> --apply-immediately`
  aurora_postgresql_deletion_protection     = local.is_prod ? true : false
  aurora_postgresql_skip_final_snapshot     = local.is_prod ? false : true
  aurora_postgresql_backup_retention_period = local.is_prod ? 7 : 1
  aurora_postgresql_instance_class          = local.is_prod ? "db.r6g.2xlarge" : "db.t4g.large"
  aws_resource_tags = {
    "AppId"       = var.app_id,
    "AppName"     = var.env_app_name,
    "Project"     = var.env_app_name,
    "Environment" = var.environment,
    "Location"    = local.default_region
  }

  teradata_ec2_instance_access_role = local.is_prod ? "arn:aws:iam::773908631549:role/TD-UTILITIES-EC2" : "arn:aws:iam::383131257218:role/TD-UTILITIES-EC2"
  red_shift_services_access_role    = local.is_prod ? "arn:aws:iam::975757171738:user/User-Prod-RedshiftServices-975757171738" : "arn:aws:iam::542502476764:user/User-NonProd-RedshiftServices-542502476764"
  erp_s3_bucket_name                = local.is_prod ? ["arn:aws:s3:::data-integration-services-input-prod/*"] : ["arn:aws:s3:::data-integration-services-input-dev/*", "arn:aws:s3:::data-integration-services-input-nonprod/*"]

  aws_account_id = {
    dev     = "007979315855"
    nonprod = "007979315855"
    prod    = "969378265367"
  }
  #https://developers.nordstromaws.app/docs/TM00458/APP09285-customer-documentation/clusters/aws/
  shared_oidc_hash = {
    dev     = "C00CF4B57472AF72A2D538476BB19575"
    nonprod = "C00CF4B57472AF72A2D538476BB19575"
    prod    = "4FEFE43EB1556516201EB0C1A4B7B518"
  }
  existing_vpc_ids = {
    dev     = "vpc-0ed8a34dc686f0ad9"
    nonprod = "vpc-0ed8a34dc686f0ad9"
    prod    = "vpc-0d70617ba8d47b8dc"
  }
  existing_subnet_ids = {
    dev     = ["subnet-07628c5c016ba782f", "subnet-0db7f6c08972d8677", "subnet-0be8e8681614d8436"]
    nonprod = ["subnet-07628c5c016ba782f", "subnet-0db7f6c08972d8677", "subnet-0be8e8681614d8436"]
    prod    = ["subnet-085ef01ac54c12526", "subnet-0d3929bd18c40d175", "subnet-0e191f7b304455a7e"]
  }
  existing_security_group_ids = {
    dev     = ["sg-068c5e283f90fde54"]
    nonprod = ["sg-068c5e283f90fde54"]
    prod    = ["sg-0a96fe00505dbcd2a"]
  }
  #https://confluence.nordstrom.com/display/PubCloud/AWS+Managed+Prefix+Lists
  nsk_prefix_list = {
    dev     = "fff000-NonProdNSKInternal-IPs"
    nonprod = "fff000-NonProdNSKInternal-IPs"
    prod    = "fff000-ProdNSKInternal-IPs"
  }

  github_runners_prefix_list = {
    dev     = "fff000-NonProdGitHubRunner-IPs"
    nonprod = "fff000-NonProdGitHubRunner-IPs"
    prod    = "fff000-ProdGitHubRunner-IPs"
  }

  ertm_data_expiration_in_days = {
    dev     = "7"
    nonprod = "30"
    prod    = "90"
  }

  db = {
    cluster_name = {
      all_clusters = "*"
    }
    username = {
      all_users = "*"
    }
  }

  ccloud_api_endpoint           = local.is_prod ? "https://ccloud-prod.nordstromaws.app/api" : "https://ccloud-nonprod.nordstromaws.app/api"
  ertm_query_source_file        = local.is_prod ? "${path.module}/../config/ertm_queries/ertm_aedw_transaction_data_extract_prod.sql" : "${path.module}/../config/ertm_queries/ertm_aedw_transaction_data_extract_nonprod.sql"
  topic_partition_count         = local.is_prod ? 3 : 1
  kafka_topic_data_retention_ms = local.is_prod ? "6480000000" : "6480000000" // 75 days

  kafka_cluster_id = {
    nonprod = "lkc-09v3v2"
    prod    = "lkc-3nwdnj"
  }
  kafka_cluster_name = {
    nonprod = "columbia-nonprod"
    prod    = "columbia-prod"
  }
  palouse_kafka_cluster_name = {
    nonprod = "palouse-nonprod"
    prod    = "palouse-prod"
  }
  restaurant_transaction_accounting_sdm_consumer_dlt_topic_name = {
    nonprod = "customer-financial-restaurant-transaction-operational-avro-data-integration-dlt"
    prod    = "customer-financial-restaurant-transaction-operational-avro-data-integration-dlt"
  }
  restaurant_transaction_accounting_sdm_consumer_retry_topic_name = {
    nonprod = "customer-financial-restaurant-transaction-operational-avro-data-integration-retry-0"
    prod    = "customer-financial-restaurant-transaction-operational-avro-data-integration-retry-0"
  }
  retail_transaction_accounting_sdm_consumer_dlt_topic_name = {
    nonprod = "customer-financial-retail-transaction-operational-avro-data-integration-dlt"
    prod    = "customer-financial-retail-transaction-operational-avro-data-integration-dlt"
  }
  retail_transaction_accounting_sdm_consumer_retry_topic_name = {
    nonprod = "customer-financial-retail-transaction-operational-avro-data-integration-retry-0"
    prod    = "customer-financial-retail-transaction-operational-avro-data-integration-retry-0"
  }
  restaurant_transaction_di_internal_topic_name = {
    nonprod = "customer-financial-restaurant-transaction-operational-di-internal-avro"
  }
  restaurant_transaction_di_internal_dlt_topic_name = {
    nonprod = "customer-financial-restaurant-transaction-operational-di-internal-avro-data-integration-dlt"
  }
  restaurant_transaction_di_internal_retry_topic_name = {
    nonprod = "customer-financial-restaurant-transaction-operational-di-internal-avro-data-integration-retry-0"
  }
  retail_transaction_di_internal_topic_name = {
    nonprod = "customer-financial-retail-transaction-operational-di-internal-avro"
  }
  retail_transaction_di_internal_dlt_topic_name = {
    nonprod = "customer-financial-retail-transaction-operational-di-internal-avro-data-integration-dlt"
  }
  retail_transaction_di_internal_retry_topic_name = {
    nonprod = "customer-financial-retail-transaction-operational-di-internal-avro-data-integration-retry-0"
  }
  nvms_payto_created_dlq_topic_name = {
    nonprod = "fintxdataintx-nvms-payto-created-dlq"
    prod    = "fintxdataintx-nvms-payto-created-dlq"
  }
  nvms_payto_updated_dlq_topic_name = {
    nonprod = "fintxdataintx-nvms-payto-updated-dlq"
    prod    = "fintxdataintx-nvms-payto-updated-dlq"
  }
  nvms_payto_created_di_internal_topic_name = {
    nonprod = "fintxdataintx-nvms-payto-created-di-internal"
  }
  nvms_payto_updated_di_internal_topic_name = {
    nonprod = "fintxdataintx-nvms-payto-updated-di-internal"
  }
  nvms_payto_created_dlq_di_internal_topic_name = {
    nonprod = "fintxdataintx-nvms-payto-created-dlq-di-internal"
  }
  nvms_payto_updated_dlq_di_internal_topic_name = {
    nonprod = "fintxdataintx-nvms-payto-updated-dlq-di-internal"
  }

  cc_topic_config = {
    "cleanup.policy"                      = "delete"
    "delete.retention.ms"                 = "86400000"
    "max.compaction.lag.ms"               = "9223372036854775807"
    "max.message.bytes"                   = "1048588"
    "message.timestamp.difference.max.ms" = "9223372036854775807"
    "message.timestamp.type"              = "CreateTime"
    "min.compaction.lag.ms"               = "0"
    "retention.bytes"                     = "-1"
    "retention.ms"                        = local.kafka_topic_data_retention_ms
  }

  cc_internal_topic_config = {
    "cleanup.policy"                      = "delete"
    "delete.retention.ms"                 = "3600000"
    "max.compaction.lag.ms"               = "9223372036854775807"
    "max.message.bytes"                   = "1048588"
    "message.timestamp.difference.max.ms" = "9223372036854775807"
    "message.timestamp.type"              = "CreateTime"
    "min.compaction.lag.ms"               = "0"
    "retention.bytes"                     = "-1"
    "retention.ms"                        = "3600000" // 1 hour
  }

  cc_nvms_topic_config = {
    "cleanup.policy"                      = "delete"
    "delete.retention.ms"                 = "86400000"
    "max.compaction.lag.ms"               = "9223372036854775807"
    "max.message.bytes"                   = "1048588"
    "message.timestamp.difference.max.ms" = "9223372036854775807"
    "message.timestamp.type"              = "CreateTime"
    "min.compaction.lag.ms"               = "0"
    "retention.bytes"                     = "-1"
    "retention.ms"                        = "604800000" // 7 days
  }
}