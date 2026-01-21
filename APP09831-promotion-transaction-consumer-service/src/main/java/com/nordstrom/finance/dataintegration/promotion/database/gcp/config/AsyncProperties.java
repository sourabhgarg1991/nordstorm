package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Immutable configuration properties for async GCP processor thread pool, mapped from
 * 'async.gcp-processor' in application.yml.
 */
@ConfigurationProperties(prefix = "async.gcp-processor")
public record AsyncProperties(
    Integer corePoolSize,
    Integer maxPoolSize,
    Integer queueCapacity,
    Integer keepAliveSeconds,
    Integer awaitTerminationSeconds) {}
