package com.nordstrom.finance.dataintegration.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.standard.CurrencyCodeV2;
import com.nordstrom.standard.MoneyV2;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyUtilityTest {

  @ParameterizedTest
  @CsvSource({"10, 500000000, 10.50", "0, 250000000, 0.25", "-5, 750000000, -4.25"})
  void testGetAmountWithLongAndInt(long units, int nanos, BigDecimal expected) {
    BigDecimal result = MoneyUtility.getAmount(units, nanos);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({"10, 500000000, 10.50", "0, 250000000, 0.25", "-5, -750000000, -5.75"})
  void testGetAmountWithString(String units, String nanos, BigDecimal expected) {
    BigDecimal result = MoneyUtility.getAmount(units, nanos);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({"10, 500000000, 10.50", "0, 250000000, 0.25", "-5, 750000000, 4.25"})
  void testGetAmountWithMoneyV2(long units, int nanos, BigDecimal expected) {
    MoneyV2 money =
        MoneyV2.newBuilder()
            .setCurrencyCode(CurrencyCodeV2.USD)
            .setUnits(units)
            .setNanos(nanos)
            .build();
    BigDecimal result = MoneyUtility.getAmount(money);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({"10.50, 10.50", "-10.50, 10.50", "0, 0"})
  void testGetAbsoluteValue(BigDecimal value, BigDecimal expected) {
    BigDecimal result = MoneyUtility.getAbsoluteValue(value);
    assertEquals(expected, result);
  }
}
