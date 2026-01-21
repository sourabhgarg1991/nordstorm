package com.nordstrom.finance.dataintegration.ertm.metric;

public enum Metric {
  SAVE_TRANSACTION_TIME("database.saveTransaction.executionTime"),
  SAVE_TRANSACTION_COUNT("database.saveTransaction.count"),
  DUPLICATE_TRANSACTION_COUNT("transaction.duplicate.count"),
  BUCKET_PROCESSING_TIME("bucket.processing.executionTime"),
  FILE_PROCESSING_TIME("file.processing.executionTime"),
  BATCH_PROCESSING_TIME("batch.processing.executionTime"),
  BATCH_PROCESSING_COUNT("batch.processing.count");
  private final String metricName;

  Metric(String metricName) {
    this.metricName = metricName;
  }

  @Override
  public String toString() {
    return this.metricName;
  }

  public String getMetricName() {
    return metricName;
  }
}
