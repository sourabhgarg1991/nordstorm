package com.nordstrom.finance.dataintegration.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.experimental.UtilityClass;

/** Utility class to support money conversions. */
@UtilityClass
public class MoneyUtility {

  private static final BigDecimal NANOS_DIVISOR = BigDecimal.valueOf(1_000_000_000);

  /**
   * Method to construct Amount using Units and Nanos.
   *
   * @param units long
   * @param nanos int
   * @return BigDecimal
   */
  public static BigDecimal getAmount(long units, int nanos) {
    return BigDecimal.valueOf(units).add(convertNanosToUnits(nanos));
  }

  /**
   * Method to construct Amount using Units and Nanos as Strings.
   *
   * @param units String
   * @param nanos String
   * @return BigDecimal
   */
  public static BigDecimal getAmount(String units, String nanos) {
    return getAmount(Long.parseLong(units), Integer.parseInt(nanos));
  }

  /**
   * Converts a given Nanos Amount to Units and rounds the result to two decimal places.
   *
   * @param nanos int
   * @return BigDecimal
   */
  private static BigDecimal convertNanosToUnits(int nanos) {
    return BigDecimal.valueOf(nanos).divide(NANOS_DIVISOR, 2, RoundingMode.HALF_UP);
  }

  /**
   * Method to extract Units from Amount.
   *
   * @param amount BigDecimal
   * @return String
   */
  public static String getUnitsFromAmount(BigDecimal amount) {
    return String.valueOf(amount.longValue());
  }

  /**
   * Method to extract Nanos from Amount.
   *
   * @param amount BigDecimal
   * @return String
   */
  public static String getNanosFromAmount(BigDecimal amount) {
    return String.valueOf(
        amount
            .remainder(BigDecimal.ONE)
            .multiply(NANOS_DIVISOR)
            .setScale(0, RoundingMode.HALF_UP)
            .longValue());
  }
}
