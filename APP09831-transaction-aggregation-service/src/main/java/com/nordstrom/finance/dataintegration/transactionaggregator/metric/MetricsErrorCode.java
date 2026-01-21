package com.nordstrom.finance.dataintegration.transactionaggregator.metric;

import lombok.Getter;

/** Enum to define metrics error codes */
@Getter
public enum MetricsErrorCode {
  QUERY_EXECUTION_ERROR("QueryExecutionError"),
  S3_UPLOAD_ERROR("S3UploadError"),
  CSV_GENERATION_ERROR("CSVGenerationError"),
  DB_SAVE_ERROR("DbSaveError"),
  DB_CONNECTION_ERROR("DbConnectionError"),
  JOB_EXECUTION_ERROR("JobExecutionError"),
  INVALID_DATA_CANNOT_BE_EMPTY("DataControlCannotBeEmpty"),
  RETRY_FILE_UPLOAD_PROCESS_EXECUTION_ERROR("retryFileUploadProcessExecutionError"),
  RETRY_FILE_UPLOAD_ERROR("retryFileUploadError");
  private final String name;

  MetricsErrorCode(String name) {
    this.name = name;
  }
}
