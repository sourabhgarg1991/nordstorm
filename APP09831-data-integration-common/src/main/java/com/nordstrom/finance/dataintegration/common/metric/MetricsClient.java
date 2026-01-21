package com.nordstrom.finance.dataintegration.common.metric;

import com.nordstrom.finance.dataintegration.common.metric.config.MetricsConfig;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A simple StatsD client implementation facilitating metrics recording using the DogStatsD client.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MetricsClient implements MetricsClientBase, Closeable {

  private final StatsDClient statsd;
  private static final String GENERAL_ERRORS_METRIC_NAME = "Errors";

  /**
   * Generates a MetricsClient using a NonBlockingStatsDClient from DogStatsD. Upon instantiation,
   * this client will establish a socket connection to a StatsD instance running on the specified
   * host and port in MetricsConfig. Metrics are then sent over this connection as they are received
   * by the client.
   */
  public static MetricsClient createClient(@NonNull MetricsConfig metricsConfig) {
    return MetricsClient.createStatsDClient(metricsConfig, new NonBlockingStatsDClientBuilder());
  }

  /**
   * Generates a MetricsClient using the provided NonBlockingStatsDClientBuilder, applying the
   * configuration provided.
   */
  protected static MetricsClient createStatsDClient(
      @NonNull MetricsConfig metricsConfig, @NonNull NonBlockingStatsDClientBuilder builder) {
    try {
      return new MetricsClient(
          builder
              .prefix(metricsConfig.getPrefix())
              .hostname(metricsConfig.getHostname())
              .port(metricsConfig.getPort())
              .constantTags(metricsConfig.getConstantTags().toArray(String[]::new))
              .originDetectionEnabled(false)
              .build());
    } catch (Exception e) {
      log.error("Failed to connect to statsD host", e);
      return new MetricsClient(new NoOpStatsDClient());
    }
  }

  /** Adds one to the value of the error metric */
  @Override
  public void incrementErrorCount(@NonNull String errorTag, String... tags) {
    List<String> tagList = new ArrayList<>(Arrays.asList(tags));
    tagList.add(errorTag);
    incrementCounter(GENERAL_ERRORS_METRIC_NAME, tagList.toArray(new String[0]));
  }

  /** Adjusts the value of the specified named counter by the specified amount. */
  @Override
  public void count(@NonNull String metricName, double delta, String... tags) {
    this.statsd.count(metricName, delta, tags);
  }

  /** Adjusts the value of the specified named counter by the specified amount. */
  @Override
  public void count(@NonNull String metricName, long delta, String... tags) {
    this.statsd.count(metricName, delta, tags);
  }

  /** Adds one to the value of the specified named counter. */
  @Override
  public void incrementCounter(@NonNull String metricName, String... tags) {
    this.statsd.incrementCounter(metricName, tags);
  }

  /** Records an execution time in milliseconds for the specified named operation. */
  @Override
  public void recordExecutionTime(@NonNull String metricName, long durationMs, String... tags) {
    this.statsd.recordExecutionTime(metricName, durationMs, tags);
  }

  /** Records the latest fixed value for the specified named gauge. */
  @Override
  public void recordGaugeValue(@NonNull String metricName, double value, String... tags) {
    this.statsd.recordGaugeValue(metricName, value, tags);
  }

  /** Records the latest fixed value for the specified named gauge. */
  @Override
  public void recordGaugeValue(@NonNull String metricName, long value, String... tags) {
    this.statsd.recordGaugeValue(metricName, value, tags);
  }

  public void close() {
    if (statsd != null) {
      statsd.close();
    }
  }
}
