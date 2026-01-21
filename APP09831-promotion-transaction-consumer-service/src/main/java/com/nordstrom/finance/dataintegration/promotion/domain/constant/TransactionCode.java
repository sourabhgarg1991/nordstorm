package com.nordstrom.finance.dataintegration.promotion.domain.constant;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Represents Transaction Types */
@Getter
@RequiredArgsConstructor
public enum TransactionCode {
  SALE("SALE"),
  RETURN("RETN"),
  VOID("VOID"),
  EXCHANGE("EXCH");
  private final String code;

  public static Optional<TransactionCode> fromCode(String code) {
    return Arrays.stream(values())
        .filter(transactionCode -> transactionCode.code.equalsIgnoreCase(code))
        .findFirst();
  }
}
