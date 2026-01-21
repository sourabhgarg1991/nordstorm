package com.nordstrom.finance.dataintegration.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DateTimeFormatUtilityTest {

  @ParameterizedTest(name = "Date {0} should format to {1}")
  @CsvSource({"2025-10-16, 2025-10-16", "2024-02-29, 2024-02-29", "2025-01-05, 2025-01-05"})
  void simpleDate_formatsAndParsesCorrectly(String dateString, String expectedFormatted) {
    LocalDate date = LocalDate.parse(dateString);

    String formattedResult = DateTimeFormatUtility.formatToSimpleDate(date);
    assertThat(formattedResult).isEqualTo(expectedFormatted);

    LocalDate parsedResult = DateTimeFormatUtility.parseSimpleDate(expectedFormatted);
    assertThat(parsedResult).isEqualTo(date);
  }

  @Test
  void parseSimpleDate_withInvalidFormat_throwsException() {
    String invalidDate = "10-16-2025";
    assertThatThrownBy(() -> DateTimeFormatUtility.parseSimpleDate(invalidDate))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  void getCurrentSimpleDate_returnsFormattedDate() {
    String result = DateTimeFormatUtility.getCurrentSimpleDate();
    assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}");
  }

  @ParameterizedTest(name = "Timestamp {0} should format to {1}")
  @CsvSource({
    // LocalDateTime.of(Year, Month, Day, Hour, Minute, Second, Nano)
    "2025, 10, 16, 14, 30, 45, 0, 2025-10-16 14:30:45.000",
    "2025, 10, 16, 14, 30, 45, 123000000, 2025-10-16 14:30:45.123",
    "2025, 10, 16, 14, 30, 45, 999000000, 2025-10-16 14:30:45.999"
  })
  void formatToTimestampMilliseconds_formatsCorrectly(
      int year, int month, int day, int hour, int minute, int second, int nano, String expected) {
    LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, second, nano);
    String result = DateTimeFormatUtility.formatToTimestampMilliseconds(dateTime);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void parseTimestampToDate_handlesBigQueryFormat() {
    String bigQueryTimestamp = "2025-10-21T00:51:05.938000";
    LocalDate expected = LocalDate.of(2025, 10, 21);

    LocalDate result = DateTimeFormatUtility.parseTimestampToDate(bigQueryTimestamp);

    assertThat(result).isEqualTo(expected);

    String formattedDate = DateTimeFormatUtility.formatToSimpleDate(result);
    assertThat(formattedDate).isEqualTo("2025-10-21");
  }

  @ParameterizedTest(name = "Timestamp {0} should format to {1}")
  @CsvSource({
    // LocalDateTime.of(Year, Month, Day, Hour, Minute, Second, Nano)
    "2025, 10, 16, 14, 30, 45, 0, 2025-10-16 14:30:45.000000",
    "2025, 10, 16, 14, 30, 45, 123456000, 2025-10-16 14:30:45.123456",
    "2025, 10, 16, 14, 30, 45, 999999000, 2025-10-16 14:30:45.999999"
  })
  void formatToTimestampMicroseconds_formatsCorrectly(
      int year, int month, int day, int hour, int minute, int second, int nano, String expected) {
    LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, second, nano);
    String result = DateTimeFormatUtility.formatToTimestampMicroseconds(dateTime);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getCurrentTimestampMilliseconds_returnsFormattedTimestamp() {
    String result = DateTimeFormatUtility.getCurrentTimestampMilliseconds();
    assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
  }

  @Test
  void getCurrentTimestampMicroseconds_returnsFormattedTimestamp() {
    String result = DateTimeFormatUtility.getCurrentTimestampMicroseconds();
    assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}");
  }

  @ParameterizedTest(name = "Compact date {0} should format to {1}")
  @CsvSource({"2025-10-16, 10162025", "2025-01-05, 01052025", "2024-12-31, 12312024"})
  void compactDate_formatsAndParsesCorrectly(String dateString, String expectedFormatted) {
    LocalDate originalDate = LocalDate.parse(dateString);

    String formatted = DateTimeFormatUtility.formatToMMDDYYYY(originalDate);
    assertThat(formatted).isEqualTo(expectedFormatted);

    LocalDate parsed = DateTimeFormatUtility.parseMMDDYYYY(formatted);
    assertThat(parsed).isEqualTo(originalDate);
  }

  @Test
  void roundTrip_timezoneConversion_maintainsValue() {
    LocalDateTime originalUtc = LocalDateTime.of(2025, 10, 16, 20, 0, 0);
    LocalDateTime pst = DateTimeFormatUtility.convertUtcToPst(originalUtc);
    LocalDateTime backToUtc = DateTimeFormatUtility.convertPstToUtc(pst);

    assertThat(backToUtc).isEqualTo(originalUtc);
    assertThat(pst).isEqualTo(LocalDateTime.of(2025, 10, 16, 13, 0, 0));
  }
}
