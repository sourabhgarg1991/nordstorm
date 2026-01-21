package com.nordstrom.finance.dataintegration.promotion.database.gcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.bigquery.BigQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {GcpConfig.class})
@EnableAutoConfiguration(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ActiveProfiles("local")
@TestPropertySource(
    properties = {
      "app.gcp.project-id=test-project-id",
      "app.gcp.dataset=test-dataset",
      "app.gcp.table-name=test-table",
      "app.gcp.page-size=1000",
      "app.gcp.nap-project-id=nap-test-id",
      "app.gcp.start-date-override=2024-01-01T00:00:00Z",
      "app.gcp.end-date-override=2024-12-31T23:59:59Z",
      "app.gcp.credentials-location=/test/path"
    })
class GcpConfigTest {

  @Autowired private ApplicationContext context;

  @Test
  void testLocalBigQueryBeanCreation() {
    assertThat(context.containsBean("localBigQuery")).isTrue();
    BigQuery bigQuery = context.getBean("localBigQuery", BigQuery.class);
    assertThat(bigQuery).isNotNull();
  }

  @Test
  void testNonLocalBigQueryBeanNotCreatedInLocalProfile() {
    assertThat(context.containsBean("bigQuery")).isFalse();
  }
}
