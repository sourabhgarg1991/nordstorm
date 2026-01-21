package com.nordstrom.finance.dataintegration.utility;

import static java.math.BigDecimal.ZERO;

import com.nordstrom.standard.MoneyV2;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtility {
  private MoneyUtility() {}

  public static BigDecimal getAmount(long units, int nanos) {
    return BigDecimal.valueOf(units).add(convertNanosToUnits(nanos));
  }

  public static BigDecimal getAmount(String units, String nanos) {
    return getAmount((long) Integer.parseInt(units), Integer.parseInt(nanos));
  }

  private static BigDecimal convertNanosToUnits(int nanos) {
    return BigDecimal.valueOf((double) nanos * Math.pow(10.0, -9.0))
        .setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal getAmount(MoneyV2 money) {
    return (null != money)
        ? getAbsoluteValue(MoneyUtility.getAmount(money.getUnits(), money.getNanos()))
        : ZERO;
  }

  public static BigDecimal getAbsoluteValue(BigDecimal value) {
    return value.abs();
  }
}
