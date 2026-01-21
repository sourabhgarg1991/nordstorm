package com.nordstrom.finance.dataintegration.mapper.constant;

import java.time.ZoneId;

public enum TimeZone {
  HAWAII_TIME("Pacific/Honolulu"),
  ALASKA_TIME("America/Anchorage"),
  CA_PACIFIC_TIME("America/Los_Angeles"),
  US_PACIFIC_TIME("America/Los_Angeles"),
  CA_MOUNTAIN_TIME("America/Edmonton"),
  US_MOUNTAIN_TIME("America/Denver"),
  CA_CENTRAL_TIME("America/Winnipeg"),
  US_CENTRAL_TIME("America/Chicago"),
  CA_EASTERN_TIME("America/Toronto"),
  US_EASTERN_TIME("America/New_York"),
  CA_ATLANTIC_TIME("America/Halifax"),
  US_ATLANTIC_TIME("America/Puerto_Rico");

  private final String timeZoneId;

  TimeZone(String timeZoneId) {
    this.timeZoneId = timeZoneId;
  }

  public ZoneId toZoneId() {
    return ZoneId.of(this.timeZoneId);
  }
}
