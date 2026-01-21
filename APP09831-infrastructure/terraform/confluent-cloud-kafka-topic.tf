resource "ccloud_kafka_topic" "customer-financial-retail-transaction-operational-dlt-topic" {
  count           = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.retail_transaction_accounting_sdm_consumer_dlt_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-retail-transaction-operational-retry-topic" {
  count           = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.retail_transaction_accounting_sdm_consumer_retry_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-restaurant-transaction-operational-dlt-topic" {
  count           = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.restaurant_transaction_accounting_sdm_consumer_dlt_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-restaurant-transaction-operational-retry-topic" {
  count           = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.restaurant_transaction_accounting_sdm_consumer_retry_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-restaurant-transaction-operational-di-internal-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.restaurant_transaction_di_internal_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-restaurant-transaction-operational-di-internal-dlt-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.restaurant_transaction_di_internal_dlt_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-restaurant-transaction-operational-di-internal-retry-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.restaurant_transaction_di_internal_retry_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-retail-transaction-operational-di-internal-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.retail_transaction_di_internal_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-retail-transaction-operational-di-internal-dlt-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.retail_transaction_di_internal_dlt_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "customer-financial-retail-transaction-operational-di-internal-retry-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.kafka_cluster_name[local.environment]
  topic_name      = local.retail_transaction_di_internal_retry_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "nvms-payto-created-dlq-topic" {
  count           = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  cluster_name    = local.palouse_kafka_cluster_name[local.environment]
  topic_name      = local.nvms_payto_created_dlq_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_nvms_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "nvms-payto-updated-dlq-topic" {
  count           = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  cluster_name    = local.palouse_kafka_cluster_name[local.environment]
  topic_name      = local.nvms_payto_updated_dlq_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_nvms_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "nvms-payto-created-di-internal-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.palouse_kafka_cluster_name[local.environment]
  topic_name      = local.nvms_payto_created_di_internal_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "nvms-payto-updated-di-internal-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.palouse_kafka_cluster_name[local.environment]
  topic_name      = local.nvms_payto_updated_di_internal_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "nvms-payto-created-dlq-di-internal-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.palouse_kafka_cluster_name[local.environment]
  topic_name      = local.nvms_payto_created_dlq_di_internal_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}

resource "ccloud_kafka_topic" "nvms-payto-updated-dlq-di-internal-topic" {
  count           = local.is_nonprod ? 1 : 0 # created only in nonprod environment
  cluster_name    = local.palouse_kafka_cluster_name[local.environment]
  topic_name      = local.nvms_payto_updated_dlq_di_internal_topic_name[local.environment]
  partition_count = local.topic_partition_count
  topic_config    = local.cc_internal_topic_config

  allow_to_manage {
    identity_pool {
      manager_id = upper(var.app_id)
    }
  }

  allow_to_read {
    identity_pool {
      reader_id = upper(var.app_id)
    }
  }

  allow_to_write {
    identity_pool {
      writer_id = upper(var.app_id)
    }
  }
}
