package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import static com.nordstrom.finance.dataintegration.promotion.database.gcp.constant.GcpQueryConstants.GCP_EXECUTOR_BEAN_NAME;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async configuration for GCP data batch processing. Uses immutable configuration properties
 * consistent with GcpConfig pattern.
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfig {

  private final AsyncProperties asyncProperties;

  @Bean(name = GCP_EXECUTOR_BEAN_NAME)
  public Executor promotionTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(asyncProperties.corePoolSize());
    executor.setMaxPoolSize(asyncProperties.maxPoolSize());
    executor.setQueueCapacity(asyncProperties.queueCapacity());
    executor.setKeepAliveSeconds(asyncProperties.keepAliveSeconds());
    executor.setThreadNamePrefix("GCP-Batch-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(asyncProperties.awaitTerminationSeconds());
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    executor.initialize();

    log.info(
        "Initialized GCP Data Processor Thread Pool - Core: {}, Max: {}, Queue: {}",
        asyncProperties.corePoolSize(),
        asyncProperties.maxPoolSize(),
        asyncProperties.queueCapacity());

    return executor;
  }
}
