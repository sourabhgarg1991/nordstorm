package com.nordstrom.finance.dataintegration.common.metric.config;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsConfig {
  /** Constant tags included with every metric. (Default: empty list) */
  private List<String> constantTags = new ArrayList<>();

  /** StatsD host name. (Required) */
  private String hostname;

  /** StatsD host port. (Required) */
  private int port;

  /** Prefix applied to all metric keys. (Default: empty string) */
  private String prefix = "";
}
