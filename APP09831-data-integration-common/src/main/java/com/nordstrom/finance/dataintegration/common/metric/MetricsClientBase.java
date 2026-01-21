package com.nordstrom.finance.dataintegration.common.metric;

import lombok.NonNull;

/** Metrics client base interface. */
public interface MetricsClientBase {

  /** Adds one to the value of the error metric */
  void incrementErrorCount(@NonNull String errorTag, String... tags);

  /** Adjusts the value of the specified named counter by the specified amount. */
  void count(@NonNull String metricName, double delta, String... tags);

  /** Adjusts the value of the specified named counter by the specified amount. */
  void count(@NonNull String metricName, long delta, String... tags);

  /** Adds one to the value of the specified named counter. */
  void incrementCounter(@NonNull String metricName, String... tags);

  /** Records an execution time in milliseconds for the specified named operation. */
  void recordExecutionTime(@NonNull String metricName, long durationMs, String... tags);

  /** Records the latest fixed value for the specified named gauge. */
  void recordGaugeValue(@NonNull String metricName, double value, String... tags);

  /** Records the latest fixed value for the specified named gauge. */
  void recordGaugeValue(@NonNull String metricName, long value, String... tags);
}
