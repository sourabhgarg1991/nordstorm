package com.nordstrom.finance.dataintegration.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringFormatUtilityTest {

  @Test
  void toFourDigitFormat_withSingleDigit_padsWithZeros() {
    String result = StringFormatUtility.toFourDigitFormat("1");
    assertThat(result).isEqualTo("0001");
  }

  @Test
  void toFourDigitFormat_withTwoDigits_padsWithZeros() {
    String result = StringFormatUtility.toFourDigitFormat("12");
    assertThat(result).isEqualTo("0012");
  }

  @Test
  void toFourDigitFormat_withThreeDigits_padsWithZeros() {
    String result = StringFormatUtility.toFourDigitFormat("123");
    assertThat(result).isEqualTo("0123");
  }

  @Test
  void toFourDigitFormat_withFourDigits_returnsAsIs() {
    String result = StringFormatUtility.toFourDigitFormat("1234");
    assertThat(result).isEqualTo("1234");
  }

  @Test
  void toFourDigitFormat_withFiveDigits_returnsAsIs() {
    String result = StringFormatUtility.toFourDigitFormat("12345");
    assertThat(result).isEqualTo("12345");
  }

  @Test
  void toFourDigitFormat_withEmptyString_returnsEmpty() {
    String result = StringFormatUtility.toFourDigitFormat("");
    assertThat(result).isEmpty();
  }

  @Test
  void toFourDigitFormat_withNonNumeric_returnsAsIs() {
    String result = StringFormatUtility.toFourDigitFormat("ABC");
    assertThat(result).isEqualTo("ABC");
  }

  @Test
  void toFourDigitFormat_withMixedAlphanumeric_returnsAsIs() {
    String result = StringFormatUtility.toFourDigitFormat("12A3");
    assertThat(result).isEqualTo("12A3");
  }

  @Test
  void isNumeric_withNumericString_returnsTrue() {
    boolean result = StringFormatUtility.isNumeric("12345");
    assertThat(result).isTrue();
  }

  @Test
  void isNumeric_withNonNumericString_returnsFalse() {
    boolean result = StringFormatUtility.isNumeric("ABC");
    assertThat(result).isFalse();
  }

  @Test
  void isNumeric_withMixedString_returnsFalse() {
    boolean result = StringFormatUtility.isNumeric("12A34");
    assertThat(result).isFalse();
  }

  @Test
  void isNumeric_withEmptyString_returnsFalse() {
    boolean result = StringFormatUtility.isNumeric("");
    assertThat(result).isFalse();
  }

  @Test
  void isFourDigits_withFourDigits_returnsTrue() {
    boolean result = StringFormatUtility.isFourDigits("1234");
    assertThat(result).isTrue();
  }

  @Test
  void isFourDigits_withThreeDigits_returnsFalse() {
    boolean result = StringFormatUtility.isFourDigits("123");
    assertThat(result).isFalse();
  }

  @Test
  void isFourDigits_withFiveDigits_returnsFalse() {
    boolean result = StringFormatUtility.isFourDigits("12345");
    assertThat(result).isFalse();
  }

  @Test
  void isFourDigits_withNonNumeric_returnsFalse() {
    boolean result = StringFormatUtility.isFourDigits("ABCD");
    assertThat(result).isFalse();
  }

  @Test
  void departmentCodeFormatting_realWorldExample() {
    String deptCode = "123";
    String formatted = StringFormatUtility.toFourDigitFormat(deptCode);
    assertThat(formatted).isEqualTo("0123");
    assertThat(StringFormatUtility.isFourDigits(formatted)).isTrue();
  }

  @Test
  void storeNumberFormatting_realWorldExample() {
    String store = "5";
    if (!StringFormatUtility.isFourDigits(store))
      store = StringFormatUtility.toFourDigitFormat(store);
    assertThat(store).isEqualTo("0005");
  }
}
