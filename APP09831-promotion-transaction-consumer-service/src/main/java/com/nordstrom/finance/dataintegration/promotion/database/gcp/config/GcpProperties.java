package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Immutable configuration properties for GCP integration, mapped from 'app.gcp' in application.yml.
 *
 * <p>This configuration is scoped for use by GCP database services.
 */
@ConfigurationProperties(prefix = "app.gcp")
public record GcpProperties(
    String startDateOverride,
    String endDateOverride,
    String napProjectId,
    String projectId,
    String dataset,
    String tableName,
    Long pageSize,
    String credentialsLocation) {}
