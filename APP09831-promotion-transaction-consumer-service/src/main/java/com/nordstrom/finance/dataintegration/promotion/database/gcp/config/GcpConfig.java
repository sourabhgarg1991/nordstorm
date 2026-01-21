package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class for GCP integration. Registers the immutable GcpProperties record for
 * property binding and provides BigQuery client beans.
 *
 * <p>For integration tests with @MockitoBean, this configuration is skipped entirely.
 */
@Configuration
@EnableConfigurationProperties(GcpProperties.class)
public class GcpConfig {

  /**
   * Creates a BigQuery client bean for non-local environments. This bean will NOT be created during
   * integration tests when BigQuery is mocked.
   *
   * @param gcpProperties the GCP configuration properties
   * @return a BigQuery client
   * @throws IOException if there is an error reading the credentials file
   */
  @Bean
  @Profile("!local & !integration-test")
  @ConditionalOnMissingBean(BigQuery.class)
  public BigQuery bigQuery(GcpProperties gcpProperties) throws IOException {
    String projectId = gcpProperties.projectId();
    String credentialsLocation = gcpProperties.credentialsLocation();

    try (InputStream stream = new FileInputStream(credentialsLocation)) {
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(stream)
              .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

      return BigQueryOptions.newBuilder()
          .setProjectId(projectId)
          .setCredentials(credentials)
          .build()
          .getService();
    }
  }

  /**
   * Creates a BigQuery client bean for local development Uses default credentials (typically from
   * gcloud CLI)
   *
   * @param gcpProperties the GCP configuration properties
   * @return a BigQuery client using default credentials
   */
  @Bean
  @Profile("local")
  @ConditionalOnMissingBean(BigQuery.class)
  public BigQuery localBigQuery(GcpProperties gcpProperties) {
    String projectId = gcpProperties.projectId();

    if (projectId != null && !projectId.isEmpty()) {
      return BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();
    } else {
      return BigQueryOptions.getDefaultInstance().getService();
    }
  }
}
