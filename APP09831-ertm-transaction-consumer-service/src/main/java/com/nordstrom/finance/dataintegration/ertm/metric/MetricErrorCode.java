package com.nordstrom.finance.dataintegration.ertm.metric;

public enum MetricErrorCode {
  AWS_S3_FILE_READ_ERROR("S3FileReadError"),
  DB_CONNECTION_ERROR("DbConnectionError"),
  AWS_S3_CONNECTION_ERROR("S3ConnectionError"),
  ENTITY_MAPPING_ERROR("EntityMappingError");

  private final String name;

  public String getErrorValue() {
    return name;
  }

  MetricErrorCode(String name) {
    this.name = name;
  }
}
