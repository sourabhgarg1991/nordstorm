package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = AsyncPropertiesTest.TestPropertiesConfig.class)
@TestPropertySource(
    properties = {
      "async.gcp-processor.core-pool-size=4",
      "async.gcp-processor.max-pool-size=8",
      "async.gcp-processor.queue-capacity=100",
      "async.gcp-processor.keep-alive-seconds=60",
      "async.gcp-processor.await-termination-seconds=30"
    })
class AsyncPropertiesTest {

  @Autowired private AsyncProperties asyncProperties;

  @Test
  void propertiesAreBoundCorrectly() {
    assertThat(asyncProperties.corePoolSize()).isEqualTo(4);
    assertThat(asyncProperties.maxPoolSize()).isEqualTo(8);
    assertThat(asyncProperties.queueCapacity()).isEqualTo(100);
    assertThat(asyncProperties.keepAliveSeconds()).isEqualTo(60);
    assertThat(asyncProperties.awaitTerminationSeconds()).isEqualTo(30);
  }

  @EnableConfigurationProperties(AsyncProperties.class)
  static class TestPropertiesConfig {
    // Enables the properties binding
  }
}
