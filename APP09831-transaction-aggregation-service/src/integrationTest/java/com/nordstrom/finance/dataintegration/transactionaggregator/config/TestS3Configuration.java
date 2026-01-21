package com.nordstrom.finance.dataintegration.transactionaggregator.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.nordstrom.finance.dataintegration.common.aws.S3Utility;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that provides a mocked S3Utility bean for integration tests. This prevents
 * actual S3 uploads during test execution and focuses testing on database operations.
 */
@TestConfiguration
@Profile("integration-test")
public class TestS3Configuration {

  /**
   * Provides a primary mock of the S3Utility singleton. The mock is pre-configured to: - Return
   * true for all uploadFile() calls (simulating successful uploads) - Do nothing when
   * closeS3Client() is called
   *
   * @return a mocked S3Utility instance
   */
  @Bean
  @Primary
  public S3Utility mockS3Utility() {
    S3Utility mockS3Utility = Mockito.mock(S3Utility.class);
    when(mockS3Utility.uploadFile(anyString(), anyString(), anyString())).thenReturn(true);
    doNothing().when(mockS3Utility).closeS3Client();
    return mockS3Utility;
  }
}
