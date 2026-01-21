package com.nordstrom.finance.dataintegration.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Utility class for date and time formatting operations.
 *
 * <p>Provides standard formatters for:
 *
 * <ul>
 *   <li>Database persistence (yyyy-MM-dd)
 *   <li>Timestamps with milliseconds/microseconds
 *   <li>Compact date format (MMddyyyy)
 *   <li>Timezone conversion between UTC and PST
 * </ul>
 *
 * <p>All methods work with {@code LocalDateTime} for Aurora PostgreSQL entity compatibility.
 */
@UtilityClass
public class DateTimeFormatUtility {

  // ==================== Constants ====================

  /** UTC timezone */
  public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

  /** Pacific Standard Time (America/Los_Angeles) */
  public static final ZoneId PST_ZONE = ZoneId.of("America/Los_Angeles");

  /** Standard date format for database persistence: yyyy-MM-dd */
  public static final DateTimeFormatter SIMPLE_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /** Compact date format: MMddyyyy */
  public static final DateTimeFormatter MMDDYYYY_FORMATTER =
      DateTimeFormatter.ofPattern("MMddyyyy");

  /** Timestamp with milliseconds: yyyy-MM-dd HH:mm:ss.SSS */
  public static final DateTimeFormatter TIMESTAMP_MILLISECONDS_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  /** Timestamp with microseconds in UTC: yyyy-MM-dd HH:mm:ss.SSSSSS */
  public static final DateTimeFormatter TIMESTAMP_MICROSECONDS_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

  // ==================== Database Persistence Format (yyyy-MM-dd) ====================

  /**
   * Formats LocalDate to standard database persistence format (yyyy-MM-dd).
   *
   * <p>Use this for storing dates in Aurora PostgreSQL.
   *
   * @param date the date to format
   * @return formatted date string (e.g., "2025-10-16")
   */
  public static String formatToSimpleDate(@NonNull LocalDate date) {
    return date.format(SIMPLE_DATE_FORMATTER);
  }

  /**
   * Parses a database date string (yyyy-MM-dd) to LocalDate.
   *
   * <p>Use this for reading dates from Aurora PostgreSQL.
   *
   * @param dateString the date string to parse (e.g., "2025-10-16")
   * @return parsed LocalDate
   */
  public static LocalDate parseSimpleDate(@NonNull String dateString) {
    return LocalDate.parse(dateString, SIMPLE_DATE_FORMATTER);
  }

  /**
   * Gets current date in database format (yyyy-MM-dd).
   *
   * @return today's date as formatted string (e.g., "2025-10-16")
   */
  public static String getCurrentSimpleDate() {
    return formatToSimpleDate(LocalDate.now());
  }

  // ==================== Timestamp Formatting ====================
  /**
   * Parses an ISO-8601 timestamp string to LocalDate.
   *
   * <p>Handles timestamps in format: yyyy-MM-ddTHH:mm:ss.SSSSSS
   *
   * <p>Extracts only the date portion, discarding time information.
   *
   * <p>Use this for converting BigQuery timestamps to date fields.
   *
   * @param timestampString the timestamp string to parse (e.g., "2025-10-21T00:51:05.938000")
   * @return parsed LocalDate (e.g., 2025-10-21)
   */
  public static LocalDate parseTimestampToDate(@NonNull String timestampString) {
    return LocalDateTime.parse(timestampString).toLocalDate();
  }

  /**
   * Formats LocalDateTime to timestamp with microseconds in UTC (yyyy-MM-dd HH:mm:ss.SSSSSS).
   *
   * <p>Use this for high-precision timestamps requiring microsecond accuracy.
   *
   * @param dateTime the datetime to format
   * @return formatted timestamp string (e.g., "2025-10-16 14:30:45.123456")
   */
  public static String formatToTimestampMicroseconds(@NonNull LocalDateTime dateTime) {
    return dateTime.atZone(UTC_ZONE).format(TIMESTAMP_MICROSECONDS_FORMATTER);
  }

  /**
   * Formats Instant to timestamp with microseconds in UTC (yyyy-MM-dd HH:mm:ss.SSSSSS).
   *
   * @param instant the instant to format
   * @return formatted timestamp string (e.g., "2025-10-16 14:30:45.123456")
   */
  public static String formatToTimestampMicroseconds(@NonNull Instant instant) {
    return TIMESTAMP_MICROSECONDS_FORMATTER.format(instant);
  }

  /**
   * Formats LocalDateTime to timestamp with milliseconds (yyyy-MM-dd HH:mm:ss.SSS).
   *
   * <p>Use this for standard logging or timestamps with millisecond precision.
   *
   * @param dateTime the datetime to format
   * @return formatted timestamp string (e.g., "2025-10-16 14:30:45.123")
   */
  public static String formatToTimestampMilliseconds(@NonNull LocalDateTime dateTime) {
    return dateTime.format(TIMESTAMP_MILLISECONDS_FORMATTER);
  }

  /**
   * Gets current timestamp with milliseconds (yyyy-MM-dd HH:mm:ss.SSS).
   *
   * @return current timestamp as formatted string
   */
  public static String getCurrentTimestampMilliseconds() {
    return formatToTimestampMilliseconds(LocalDateTime.now());
  }

  /**
   * Gets current timestamp with microseconds in UTC (yyyy-MM-dd HH:mm:ss.SSSSSS).
   *
   * @return current timestamp as formatted string
   */
  public static String getCurrentTimestampMicroseconds() {
    return formatToTimestampMicroseconds(Instant.now());
  }

  // ==================== Compact Date Format (MMddyyyy) ====================

  /**
   * Formats LocalDate to compact format (MMddyyyy).
   *
   * <p>Use this for file naming or legacy system integration.
   *
   * @param date the date to format
   * @return formatted date string (e.g., "10162025")
   */
  public static String formatToMMDDYYYY(@NonNull LocalDate date) {
    return date.format(MMDDYYYY_FORMATTER);
  }

  /**
   * Parses a compact date string (MMddyyyy) to LocalDate.
   *
   * @param dateString the date string to parse (e.g., "10162025")
   * @return parsed LocalDate
   */
  public static LocalDate parseMMDDYYYY(@NonNull String dateString) {
    return LocalDate.parse(dateString, MMDDYYYY_FORMATTER);
  }

  // ==================== Timezone Conversion ====================

  /**
   * Converts LocalDateTime from UTC to PST timezone.
   *
   * @param utcDateTime the datetime in UTC
   * @return datetime in PST
   */
  public static LocalDateTime convertUtcToPst(@NonNull LocalDateTime utcDateTime) {
    return utcDateTime.atZone(UTC_ZONE).withZoneSameInstant(PST_ZONE).toLocalDateTime();
  }

  /**
   * Converts LocalDateTime from PST to UTC timezone.
   *
   * @param pstDateTime the datetime in PST
   * @return datetime in UTC
   */
  public static LocalDateTime convertPstToUtc(@NonNull LocalDateTime pstDateTime) {
    return pstDateTime.atZone(PST_ZONE).withZoneSameInstant(UTC_ZONE).toLocalDateTime();
  }
}
