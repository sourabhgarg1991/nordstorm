package com.nordstrom.finance.dataintegration.common.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Utility class for string formatting operations.
 *
 * <p>Provides formatting for department codes, fee codes, and store numbers that require 4-digit
 * format with leading zeros.
 */
@UtilityClass
public class StringFormatUtility {

  private static final String NUMERIC_PATTERN = "\\d+";

  /**
   * Formats a numeric string to 4 digits by padding with leading zeros.
   *
   * <p>Use this for department codes, fee codes, and store numbers.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>"123" → "0123"
   *   <li>"1" → "0001"
   *   <li>"1234" → "1234" (already 4 digits, no change)
   *   <li>"" → "" (empty string returned as-is)
   *   <li>"ABC" → "ABC" (non-numeric string returned as-is)
   * </ul>
   *
   * @param input the numeric string to format
   * @return formatted string with leading zeros, or original string if not numeric or already 4
   *     digits
   */
  public static String toFourDigitFormat(@NonNull String input) {
    if (input.isEmpty() || !input.matches(NUMERIC_PATTERN)) {
      return input;
    }

    if (input.length() >= 4) {
      return input;
    }

    return String.format("%04d", Integer.parseInt(input));
  }

  /**
   * Checks if a string contains only numeric characters.
   *
   * @param input the string to check
   * @return true if string contains only digits, false otherwise
   */
  public static boolean isNumeric(@NonNull String input) {
    return !input.isEmpty() && input.matches(NUMERIC_PATTERN);
  }

  /**
   * Checks if a string is exactly 4 digits.
   *
   * <p>Use this to validate department codes, fee codes, or store numbers.
   *
   * @param input the string to check
   * @return true if string is exactly 4 digits, false otherwise
   */
  public static boolean isFourDigits(@NonNull String input) {
    return input.length() == 4 && input.matches(NUMERIC_PATTERN);
  }
}
