package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = GcpPropertiesTest.TestPropertiesConfig.class)
@TestPropertySource(
    properties = {
      "app.gcp.project-id=test-main-project-id",
      "app.gcp.nap-project-id=test-nap-project-id",
      "app.gcp.dataset=test-dataset",
      "app.gcp.table-name=test-table",
      "app.gcp.page-size=1000",
      "app.gcp.credentials-location=/test/path/to/credentials.json",
      "app.gcp.start-date-override=2023-01-01T00:00:00Z",
      "app.gcp.end-date-override=2023-12-31T23:59:59Z"
    })
class GcpPropertiesTest {

  @Autowired private GcpProperties gcpProperties;

  @Test
  void propertiesAreBoundCorrectly() {
    assertThat(gcpProperties.projectId()).isEqualTo("test-main-project-id");
    assertThat(gcpProperties.napProjectId()).isEqualTo("test-nap-project-id");
    assertThat(gcpProperties.dataset()).isEqualTo("test-dataset");
    assertThat(gcpProperties.tableName()).isEqualTo("test-table");
    assertThat(gcpProperties.pageSize()).isEqualTo(1000L);
    assertThat(gcpProperties.credentialsLocation()).isEqualTo("/test/path/to/credentials.json");
    assertThat(gcpProperties.startDateOverride()).isEqualTo("2023-01-01T00:00:00Z");
    assertThat(gcpProperties.endDateOverride()).isEqualTo("2023-12-31T23:59:59Z");
  }

  /** Minimal configuration just for testing property binding */
  @EnableConfigurationProperties(GcpProperties.class)
  static class TestPropertiesConfig {
    // This class just enables the properties binding
  }
}
