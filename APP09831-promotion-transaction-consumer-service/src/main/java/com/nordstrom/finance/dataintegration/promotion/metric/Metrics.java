package com.nordstrom.finance.dataintegration.promotion.metric;

import lombok.Getter;

/** Enum to define metric keys used for successful operations */
@Getter
public enum Metrics {
  JOB_EXECUTION_TIME("job.executionTime"),
  GCP_FETCH_COUNT("gcp.fetch.count"),
  GCP_PAGE_FETCH_TIME("gcp.page.fetch.executionTime"),
  BATCH_PROCESSING_TIME("batch.processing.executionTime"),
  DUPLICATE_TRANSACTION_COUNT("transaction.duplicate.count"),
  PERSISTED_TRANSACTION_COUNT("transaction.persisted.count"),
  PERSISTENCE_TIME("persistence.executionTime");

  private final String metricName;

  Metrics(String metricName) {
    this.metricName = metricName;
  }
}
