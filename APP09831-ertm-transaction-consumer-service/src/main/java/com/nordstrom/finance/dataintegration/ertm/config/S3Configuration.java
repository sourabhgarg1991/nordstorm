package com.nordstrom.finance.dataintegration.ertm.config;

import com.nordstrom.finance.dataintegration.common.aws.S3Utility;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for S3Utility bean. Provides the common-lib S3Utility singleton as a Spring-managed
 * bean to enable dependency injection and testing.
 *
 * <p>This configuration is only active when the "integration-test" profile is NOT active. In
 * integration tests, TestS3Configuration provides a mock bean instead.
 */
@Configuration
@Profile("!integrationTest")
public class S3Configuration {

  /**
   * Provides the S3Utility singleton instance as a Spring bean. This allows the singleton to be
   * injected via constructor injection.
   *
   * <p>This bean is not created during integration tests (profile="integration-test"), where a mock
   * is used instead.
   *
   * @return the S3Utility singleton instance from common-lib
   */
  @Bean
  public S3Utility s3Utility() {
    return S3Utility.getInstance();
  }
}
