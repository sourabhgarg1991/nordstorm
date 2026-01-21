package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

  private AsyncConfig asyncConfig;

  @BeforeEach
  void setUp() {
    AsyncProperties asyncProperties = new AsyncProperties(5, 10, 50, 60, 120);
    asyncConfig = new AsyncConfig(asyncProperties);
  }

  @Test
  void testPromotionTaskExecutor_CreatesExecutorWithCorrectProperties() {
    Executor executor = asyncConfig.promotionTaskExecutor();

    assertThat(executor).isNotNull();
    assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

    ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
    assertThat(taskExecutor.getCorePoolSize()).isEqualTo(5);
    assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(10);
    assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("GCP-Batch-");
    assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(60);
  }

  @Test
  void testPromotionTaskExecutor_InitializesExecutor() {
    Executor executor = asyncConfig.promotionTaskExecutor();

    ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
    assertThat(taskExecutor.getActiveCount()).isEqualTo(0);
    assertThat(taskExecutor.getPoolSize()).isGreaterThanOrEqualTo(0);
    assertThat(taskExecutor.getThreadPoolExecutor().isShutdown()).isFalse();
    assertThat(taskExecutor.getThreadPoolExecutor().isTerminated()).isFalse();
  }

  @Test
  void testPromotionTaskExecutor_HasCorrectQueueCapacity() {
    Executor executor = asyncConfig.promotionTaskExecutor();

    ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
    assertThat(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(50);
  }
}
