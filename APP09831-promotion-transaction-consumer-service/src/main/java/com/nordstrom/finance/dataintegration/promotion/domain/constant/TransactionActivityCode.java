package com.nordstrom.finance.dataintegration.promotion.domain.constant;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Represents Transaction Types */
@Getter
@RequiredArgsConstructor
public enum TransactionActivityCode {
  SALE("SALE"),
  RETURN("RETN");
  private final String activityCode;

  public static Optional<TransactionActivityCode> fromActivityCode(String activityCode) {
    if (activityCode == null) {
      return Optional.empty();
    }

    // normalize input by trimming and converting to uppercase
    return switch (activityCode.trim().toUpperCase()) {
      case "S", "SALE" -> Optional.of(SALE);
      case "R", "RETURN", "RETN" -> Optional.of(RETURN);
      default -> Optional.empty();
    };
  }
}
