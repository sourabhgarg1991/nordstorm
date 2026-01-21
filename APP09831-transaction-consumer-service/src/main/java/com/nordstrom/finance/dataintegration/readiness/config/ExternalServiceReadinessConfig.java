package com.nordstrom.finance.dataintegration.readiness.config;

import com.nordstrom.finance.dataintegration.readiness.CustomReadinessCheckHealthIndicator;
import com.nordstrom.finance.dataintegration.readiness.KafkaServiceReadinessChecker;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"!integrationtest & !loadtest"})
public class ExternalServiceReadinessConfig {

  @Bean
  public CustomReadinessCheckHealthIndicator kafkaReadinessHealthIndicator(
      ApplicationAvailability availability, KafkaServiceReadinessChecker kafkaReadinessCheck) {
    return new CustomReadinessCheckHealthIndicator(availability, kafkaReadinessCheck);
  }
}
