package com.nordstrom.finance.dataintegration.promotion.config;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("integration-test")
public class TestMetricsConfiguration {

  /**
   * Provides a primary mock of the MetricsClient. All autowired dependencies requesting
   * MetricsClient will receive this mock instance.
   *
   * @return a mocked MetricsClient
   */
  @Bean
  @Primary
  public MetricsClient mockMetricsClient() {
    return Mockito.mock(MetricsClient.class);
  }
}
