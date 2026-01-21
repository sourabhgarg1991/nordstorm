package com.nordstrom.finance.dataintegration.ertm.readiness.config;

import com.nordstrom.finance.dataintegration.ertm.readiness.AuroraDBReadinessChecker;
import com.nordstrom.finance.dataintegration.ertm.readiness.CustomReadinessCheckHealthIndicator;
import com.nordstrom.finance.dataintegration.ertm.readiness.S3BucketReadinessChecker;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"!integrationTest & !loadTest & !test"})
public class ExternalServiceReadinessConfig {
  @Bean
  public CustomReadinessCheckHealthIndicator auroraDbReadinessIndicator(
      ApplicationAvailability availability, AuroraDBReadinessChecker auroraDBReadinessChecker) {
    return new CustomReadinessCheckHealthIndicator(availability, auroraDBReadinessChecker);
  }

  @Bean
  public CustomReadinessCheckHealthIndicator s3BucketReadinessIndicator(
      ApplicationAvailability availability, S3BucketReadinessChecker s3BucketReadinessChecker) {
    return new CustomReadinessCheckHealthIndicator(availability, s3BucketReadinessChecker);
  }
}
