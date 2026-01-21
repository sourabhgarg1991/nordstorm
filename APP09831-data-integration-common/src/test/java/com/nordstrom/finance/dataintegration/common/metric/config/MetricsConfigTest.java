package com.nordstrom.finance.dataintegration.common.metric.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricsConfigTest {

  @Test
  void constructor_createsConfigWithDefaults() {
    MetricsConfig config = new MetricsConfig();

    assertThat(config.getConstantTags()).isEmpty();
    assertThat(config.getPrefix()).isEmpty();
    assertThat(config.getHostname()).isNull();
    assertThat(config.getPort()).isZero();
  }

  @Test
  void allArgsConstructor_setsAllFields() {
    MetricsConfig config =
        new MetricsConfig(
            Arrays.asList("env:prod", "app:data-integration"),
            "test.kube-system.svc",
            8125,
            "data-integration");

    assertThat(config.getHostname()).isEqualTo("test.kube-system.svc");
    assertThat(config.getPort()).isEqualTo(8125);
    assertThat(config.getPrefix()).isEqualTo("data-integration");
    assertThat(config.getConstantTags()).containsExactly("env:prod", "app:data-integration");
  }

  @Test
  void settersAndGetters_workCorrectly() {
    MetricsConfig config = new MetricsConfig();

    config.setHostname("test.kube-system.svc");
    config.setPort(8125);
    config.setPrefix("test");
    config.setConstantTags(List.of("env:test"));

    assertThat(config.getHostname()).isEqualTo("test.kube-system.svc");
    assertThat(config.getPort()).isEqualTo(8125);
    assertThat(config.getPrefix()).isEqualTo("test");
    assertThat(config.getConstantTags()).hasSize(1);
  }
}
