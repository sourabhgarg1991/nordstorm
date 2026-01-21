package com.nordstrom.finance.dataintegration.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyUtilityTest {

  @ParameterizedTest(name = "units={0}, nanos={1} should return {2}")
  @CsvSource({
    "10, 500000000, 10.50",
    "25, 0, 25.00",
    "-10, 500000000, -9.50",
    "0, 100000000, 0.10",
    "5, 10000000, 5.01",
    "1, 123456789, 1.12",
    "1, 567891234, 1.57"
  })
  void getAmount_withLongIntParameters_returnsCorrectAmount(
      long units, int nanos, BigDecimal expectedAmount) {
    BigDecimal result = MoneyUtility.getAmount(units, nanos);
    assertThat(result).isEqualByComparingTo(expectedAmount);
  }

  @ParameterizedTest(name = "units=''{0}'', nanos=''{1}'' should return {2}")
  @CsvSource({"15, 250000000, 15.25", "0, 999000000, 1.00", "-5, 123456789, -4.88"})
  void getAmount_withStringParameters_returnsCorrectAmount(
      String units, String nanos, BigDecimal expectedAmount) {
    BigDecimal result = MoneyUtility.getAmount(units, nanos);
    assertThat(result).isEqualByComparingTo(expectedAmount);
  }

  @Test
  void getAmount_withInvalidStringUnits_throwsNumberFormatException() {
    String units = "invalid";
    String nanos = "100000000";
    assertThatThrownBy(() -> MoneyUtility.getAmount(units, nanos))
        .isInstanceOf(NumberFormatException.class)
        .hasMessageContaining("For input string: \"invalid\"");
  }

  @Test
  void getAmount_withInvalidStringNanos_throwsNumberFormatException() {
    String units = "10";
    String nanos = "invalid";
    assertThatThrownBy(() -> MoneyUtility.getAmount(units, nanos))
        .isInstanceOf(NumberFormatException.class)
        .hasMessageContaining("For input string: \"invalid\"");
  }

  @ParameterizedTest(name = "amount {0} should return units ''{1}''")
  @CsvSource({"123.45, 123", "50, 50", "0.99, 0", "-10.50, -10", "0.0, 0"})
  void getUnitsFromAmount_returnsCorrectUnits(BigDecimal amount, String expectedUnits) {
    String result = MoneyUtility.getUnitsFromAmount(amount);
    assertThat(result).isEqualTo(expectedUnits);
  }

  @ParameterizedTest(name = "amount {0} should return nanos ''{1}''")
  @CsvSource({
    "10.50, 500000000",
    "100, 0",
    "5.01, 10000000",
    "0.999999999, 999999999",
    "-10.50, -500000000",
    "0.001, 1000000"
  })
  void getNanosFromAmount_returnsCorrectNanos(BigDecimal amount, String expectedNanos) {
    String result = MoneyUtility.getNanosFromAmount(amount);
    assertThat(result).isEqualTo(expectedNanos);
  }

  @Test
  void roundTrip_convertsAmountToUnitsNanosAndBack() {
    BigDecimal originalAmount = new BigDecimal("12345.67");
    String units = MoneyUtility.getUnitsFromAmount(originalAmount);
    String nanos = MoneyUtility.getNanosFromAmount(originalAmount);
    BigDecimal reconstructedAmount = MoneyUtility.getAmount(units, nanos);
    assertThat(reconstructedAmount).isEqualByComparingTo(originalAmount);
  }
}
