package com.nordstrom.finance.dataintegration.metric;

public enum Metric {
  SAVE_TRANSACTION_TIME("database.saveTransaction.executionTime"),
  SAVE_TRANSACTION_COUNT("database.saveTransaction.count"),
  DUPLICATE_TRANSACTION_COUNT("transaction.duplicate.count"),
  SDM_EVENTS_CONSUMED_COUNT("consumer.sdmEvents.count"),
  DLT_EVENTS_RECOVERED_COUNT("dlt.recovered.count"),
  SDM_EVENT_FILTER_COUNT("consumer.filter.count"),
  PROCESSOR_EXECUTION_TIME("processor.executionTime");
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
