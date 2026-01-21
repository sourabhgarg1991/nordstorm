package com.nordstrom.finance.dataintegration.ertm.metric;

public enum MetricTag {
  FILE_NAME("file.name"),
  RECORDS_COUNT("records.count"),
  BATCH_NUMBER("batch.number");
  private final String tagKey;

  MetricTag(String tagKey) {
    this.tagKey = tagKey;
  }

  public String getTag(String tagValue) {
    return this.tagKey + ":" + tagValue;
  }
}
