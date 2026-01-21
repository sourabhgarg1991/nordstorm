package com.nordstrom.finance.dataintegration.common.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.nordstrom.finance.dataintegration.common.metric.config.MetricsConfig;
import com.timgroup.statsd.StatsDClient;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsClientTest {

  @Mock private StatsDClient mockStatsDClient;

  private MetricsConfig metricsConfig;
  private MetricsClient metricsClient;

  @BeforeEach
  void setUp() {
    metricsConfig = new MetricsConfig();
    metricsConfig.setHostname("test.kube-system.svc");
    metricsConfig.setPort(8125);
    metricsConfig.setPrefix("data-integration");
    metricsConfig.setConstantTags(Arrays.asList("env:test", "app:data-integration-common"));
    metricsClient = new MetricsClient(mockStatsDClient);
  }

  // Provides different scenarios for counter tags
  static Stream<Arguments> incrementCounterTagData() {
    return Stream.of(
        // Case 1: Single Tag
        Arguments.of((Object) new String[] {MetricsCommonTag.TOPIC_NAME.getTag("payments")}),
        // Case 2: Multiple Tags
        Arguments.of(
            (Object)
                new String[] {
                  MetricsCommonTag.TOPIC_NAME.getTag("users"),
                  MetricsCommonTag.ERROR_CODE.getTag("404")
                }),
        // Case 3: No Tags (empty array)
        Arguments.of((Object) new String[0]));
  }

  @Test
  void createClient_withValidConfig_createsClientSuccessfully() {
    MetricsClient client = MetricsClient.createClient(metricsConfig);
    assertThat(client).isNotNull();
  }

  @Test
  void incrementErrorCount_incrementsErrorMetricWithTag() {
    metricsClient.incrementErrorCount(MetricsCommonTag.ERROR_CODE.getTag("404"));
    verify(mockStatsDClient).incrementCounter(eq("Errors"), eq(new String[] {"error.code:404"}));
  }

  @Test
  void count_withDoubleValue_delegatesToStatsDClient() {
    metricsClient.count("request.latency", 0.5);
    verify(mockStatsDClient).count("request.latency", 0.5);
  }

  @Test
  void count_withLongValue_delegatesToStatsDClient() {
    metricsClient.count("request.count", 10L);
    verify(mockStatsDClient).count("request.count", 10L);
  }

  @Test
  void incrementCounter_delegatesToStatsDClient_withoutTags() {
    metricsClient.incrementCounter("page.view");
    verify(mockStatsDClient).incrementCounter("page.view");
  }

  @ParameterizedTest(name = "incrementCounter with tags: {0}")
  @MethodSource("incrementCounterTagData")
  void incrementCounter_delegatesToStatsDClient_withTags(String[] tags) {
    String metricName = "message.processed";
    metricsClient.incrementCounter(metricName, tags);
    verify(mockStatsDClient).incrementCounter(eq(metricName), eq(tags));
  }

  @Test
  void recordExecutionTime_delegatesToStatsDClient() {
    metricsClient.recordExecutionTime("query.duration", 250L);
    verify(mockStatsDClient).recordExecutionTime("query.duration", 250L);
  }

  @Test
  void recordGaugeValue_withDoubleValue_delegatesToStatsDClient() {
    metricsClient.recordGaugeValue("connection.pool.size", 72.5);
    verify(mockStatsDClient).recordGaugeValue("connection.pool.size", 72.5);
  }

  @Test
  void recordGaugeValue_withLongValue_delegatesToStatsDClient() {
    metricsClient.recordGaugeValue("queue.size", 100L);
    verify(mockStatsDClient).recordGaugeValue("queue.size", 100L);
  }

  @Test
  void close_closesStatsDClient() {
    metricsClient.close();
    verify(mockStatsDClient).close();
  }
}
