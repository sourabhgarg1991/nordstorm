package com.nordstrom.finance.dataintegration.transactionaggregator.config;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that provides a mocked MetricsClient bean for integration tests. This prevents
 * actual metrics from being sent during test execution.
 */
@TestConfiguration
@Profile("integration-test")
public class TestMetricsConfiguration {

  /**
   * Provides a primary mock of the MetricsClient. All autowired dependencies requesting
   * MetricsClient will receive this mock instance.
   *
   * @return a mocked MetricsClient that does nothing
   */
  @Bean
  @Primary
  public MetricsClient mockMetricsClient() {
    return Mockito.mock(MetricsClient.class);
  }
}
