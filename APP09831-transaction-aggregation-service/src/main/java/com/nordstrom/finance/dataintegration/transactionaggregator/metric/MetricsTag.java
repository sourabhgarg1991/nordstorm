package com.nordstrom.finance.dataintegration.transactionaggregator.metric;

/** Enum to define metrics tags */
public enum MetricsTag {
  FILE_NAME_PREFIX("file.name.prefix"),
  FILE_NAME("file.name"),
  BUCKET_NAME("bucket.name"),

  STATUS("status");
  private final String tagKey;

  MetricsTag(String tagKey) {
    this.tagKey = tagKey;
  }

  public String getTag(String tagValue) {
    return this.tagKey + ":" + tagValue;
  }
}
