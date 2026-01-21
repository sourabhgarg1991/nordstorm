package com.nordstrom.finance.dataintegration.promotion.metric;

import lombok.Getter;

/** Enum to define metric error codes */
@Getter
public enum MetricsErrorCode {
  GCP_QUERY_ERROR("GcpQueryError"),
  GCP_QUERY_JOB_CREATE_ERROR("GcpQueryJobCreateError"),
  GCP_MAPPING_ERROR("GcpMappingError"),
  BATCH_PROCESSING_ERROR("BatchProcessingError"),
  PERSISTENCE_ERROR("PersistenceError"),
  DB_CONNECTION_ERROR("DbConnectionError"),
  JOB_EXECUTION_ERROR("JobExecutionError");

  private final String name;

  MetricsErrorCode(String name) {
    this.name = name;
  }
}
