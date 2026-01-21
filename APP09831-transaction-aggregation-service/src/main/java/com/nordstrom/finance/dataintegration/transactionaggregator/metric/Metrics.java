package com.nordstrom.finance.dataintegration.transactionaggregator.metric;

import lombok.Getter;

/** Enum to define metrics keys used for successful operations */
@Getter
public enum Metrics {
  JOB_EXECUTION_TIME("job.executionTime"),
  CONFIGURATION_COUNT("configuration.count"),
  CONFIGURATION_EXECUTION_STATUS("configuration.status"),
  AGGREGATION_ROW_COUNT("aggregation.row.count"),
  QUERY_EXECUTION_TIME("query.executionTime"),
  CONTROL_QUERY_EXECUTION_TIME("control.query.executionTime"),
  S3_UPLOAD_STATUS("s3.upload.status"),
  S3_UPLOAD_TIME("s3.upload.executionTime"),
  DB_SAVE_TIME("db.save.executionTime"),
  CONTROL_FILE_DB_SAVE_TIME("control.file.db.save.executionTime"),
  FILE_GENERATED_COUNT("file.generated.count"),
  CONTROL_FILE_GENERATED_COUNT("control.file.generated.count");
  private final String metricName;

  Metrics(String metricName) {
    this.metricName = metricName;
  }
}
