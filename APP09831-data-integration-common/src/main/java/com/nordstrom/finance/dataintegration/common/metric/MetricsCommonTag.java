package com.nordstrom.finance.dataintegration.common.metric;

public enum MetricsCommonTag {
  ERROR_CODE("error.code"),
  TOPIC_NAME("topic.name");

  private final String tagKey;

  MetricsCommonTag(String tagKey) {
    this.tagKey = tagKey;
  }

  public String getTag(String tagValue) {
    return this.tagKey + ":" + tagValue;
  }
}
