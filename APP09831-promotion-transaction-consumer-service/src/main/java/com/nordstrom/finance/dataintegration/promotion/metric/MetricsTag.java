package com.nordstrom.finance.dataintegration.promotion.metric;

/** Enum to define metrics tags */
public enum MetricsTag {
  PAGE_NUMBER("page.number"),
  BATCH_SIZE("batch.size");
  private final String tagKey;

  MetricsTag(String tagKey) {
    this.tagKey = tagKey;
  }

  public String getTag(String tagValue) {
    return this.tagKey + ":" + tagValue;
  }
}
