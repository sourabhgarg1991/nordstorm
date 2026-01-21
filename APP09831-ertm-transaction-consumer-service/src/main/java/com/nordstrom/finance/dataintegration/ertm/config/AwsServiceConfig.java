package com.nordstrom.finance.dataintegration.ertm.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AwsServiceConfig {

  @Value("${aws.s3.bucket.source}")
  private String sourceBucket;

  @Value("${aws.s3.bucket.processed}")
  private String processedBucket;
}
