package com.nordstrom.finance.dataintegration.ertm.metric.config;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.config.MetricsConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/** Configuration class to define bean relate to Metrics */
@Configuration
public class AppMetricConfig {

  @Bean
  @ConfigurationProperties(prefix = "metrics", ignoreUnknownFields = false)
  public MetricsConfig metricsConfig() {
    return new MetricsConfig();
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
  public MetricsClient metricsClient(final MetricsConfig config) {
    return MetricsClient.createClient(config);
  }
}
