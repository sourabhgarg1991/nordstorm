package com.nordstrom.finance.dataintegration.metric;

public enum MetricTag {
  FILTER_REASON("filter.reason"),
  EVENT_TYPE("event.type");
  private final String tagKey;

  MetricTag(String tagKey) {
    this.tagKey = tagKey;
  }

  public String getTag(String tagValue) {
    return this.tagKey + ":" + tagValue;
  }
}
