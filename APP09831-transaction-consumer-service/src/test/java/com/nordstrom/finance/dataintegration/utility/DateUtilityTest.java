package com.nordstrom.finance.dataintegration.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nordstrom.standard.TimezoneId;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateUtilityTest {

  @Test
  void testToLocalDateWithValidInstant() {
    Instant instant = Instant.parse("2023-11-01T10:15:30.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    LocalDate result = DateUtility.toLocalDate(instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testToLocalDateWithNullInstant() {
    Instant instant = null;
    LocalDate result = DateUtility.toLocalDate(instant);
    assertNull(result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_hawaii() {
    Instant instant = Instant.parse("2023-11-02T10:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.HAWAII_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_hawaii_edge() {
    Instant instant = Instant.parse("2023-11-02T09:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.HAWAII_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_alaska() {
    Instant instant = Instant.parse("2023-11-02T08:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.ALASKA_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_alaska_edge() {
    Instant instant = Instant.parse("2023-11-02T07:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.ALASKA_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_pacific() {
    Instant instant = Instant.parse("2023-11-02T07:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.CA_PACIFIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_pacific_edge() {
    Instant instant = Instant.parse("2023-11-02T06:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.CA_PACIFIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_pacific() {
    Instant instant = Instant.parse("2023-11-02T07:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.US_PACIFIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_pacific_edge() {
    Instant instant = Instant.parse("2023-11-02T06:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.US_PACIFIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_mountain() {
    Instant instant = Instant.parse("2023-11-02T06:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.CA_MOUNTAIN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_mountain_edge() {
    Instant instant = Instant.parse("2023-11-02T05:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.CA_MOUNTAIN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_mountain() {
    Instant instant = Instant.parse("2023-11-02T06:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.US_MOUNTAIN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_mountain_edge() {
    Instant instant = Instant.parse("2023-11-02T05:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.US_MOUNTAIN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_central() {
    Instant instant = Instant.parse("2023-11-02T05:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.CA_CENTRAL_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_central_edge() {
    Instant instant = Instant.parse("2023-11-02T04:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.CA_CENTRAL_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_central() {
    Instant instant = Instant.parse("2023-11-02T05:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.US_CENTRAL_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_central_edge() {
    Instant instant = Instant.parse("2023-11-02T04:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.US_CENTRAL_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_eastern() {
    Instant instant = Instant.parse("2023-11-02T04:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.CA_EASTERN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_eastern_edge() {
    Instant instant = Instant.parse("2023-11-02T03:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.CA_EASTERN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_eastern() {
    Instant instant = Instant.parse("2023-11-02T04:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.US_EASTERN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_eastern_edge() {
    Instant instant = Instant.parse("2023-11-02T03:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.US_EASTERN_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_atlantic() {
    Instant instant = Instant.parse("2023-11-02T03:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.CA_ATLANTIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_ca_atlantic_edge() {
    Instant instant = Instant.parse("2023-11-02T02:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.CA_ATLANTIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_atlantic() {
    Instant instant = Instant.parse("2023-11-02T04:00:00.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 2);
    TimezoneId timezoneId = TimezoneId.US_ATLANTIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_us_atlantic_edge() {
    Instant instant = Instant.parse("2023-11-02T03:59:59.00Z");
    LocalDate expectedDate = LocalDate.of(2023, 11, 1);
    TimezoneId timezoneId = TimezoneId.US_ATLANTIC_TIME;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_unknown() {
    Instant instant = Instant.now();
    LocalDate expectedDate = LocalDate.now();
    TimezoneId timezoneId = TimezoneId.UNKNOWN;
    LocalDate result = DateUtility.updateTimeZoneAndGetDate(timezoneId, instant);
    assertEquals(expectedDate, result);
  }

  @Test
  void testUpdateTimeZoneAndGetDate_null() {
    Instant instant = Instant.now();
    LocalDate expectedDate = LocalDate.now();

    LocalDate result = DateUtility.updateTimeZoneAndGetDate(null, instant);
    assertEquals(expectedDate, result);
  }
}
