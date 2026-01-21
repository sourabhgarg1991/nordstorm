package com.nordstrom.finance.dataintegration.facade.schema.standard;

import com.nordstrom.event.secure.TokenizedValue;
import com.nordstrom.standard.CurrencyCodeV2;
import com.nordstrom.standard.TokenizedMoneyV2;
import java.util.function.UnaryOperator;

public class TokenizedMoneyBuilderFacade {

  private TokenizedMoneyBuilderFacade() {}

  private static TokenizedMoneyV2.Builder getDefaultBuilder() {
    TokenizedValue unitToken =
        TokenizedValueBuilderFacade.build(token -> token.setValue("M3GgO3SDTtmEp-g6Se6pHg"));
    TokenizedValue nanoToken =
        TokenizedValueBuilderFacade.build(token -> token.setValue("dcDZ2h8xRuKjfPx63NJpZw"));

    return TokenizedMoneyV2.newBuilder()
        .setCurrencyCode(CurrencyCodeV2.USD)
        .setUnits(unitToken)
        .setNanos(nanoToken);
  }

  public static TokenizedMoneyV2 build(UnaryOperator<TokenizedMoneyV2.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static TokenizedMoneyV2 buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static TokenizedMoneyV2 buildRevRecognizedItem1() {
    TokenizedValue unitToken =
        TokenizedValueBuilderFacade.build(
            token -> token.setValue("_iCODOfpQduGcW0PS2xgLA").setAuthority("GeneralLedger"));
    TokenizedValue nanoToken =
        TokenizedValueBuilderFacade.build(
            token -> token.setValue("UmC8L1nxRRqLXv3TsNw0ag").setAuthority("GeneralLedger"));

    return TokenizedMoneyV2.newBuilder()
        .setCurrencyCode(CurrencyCodeV2.USD)
        .setUnits(unitToken)
        .setNanos(nanoToken)
        .build();
  }

  public static TokenizedMoneyV2 buildRevRecognizedItem2() {
    TokenizedValue unitToken =
        TokenizedValueBuilderFacade.build(
            token -> token.setValue("EsDZxuYuT5CY1Q-zz7iNow").setAuthority("GeneralLedger"));
    TokenizedValue nanoToken =
        TokenizedValueBuilderFacade.build(
            token -> token.setValue("D51kX8MzQyC-54dnVYercA").setAuthority("GeneralLedger"));

    return TokenizedMoneyV2.newBuilder()
        .setCurrencyCode(CurrencyCodeV2.USD)
        .setUnits(unitToken)
        .setNanos(nanoToken)
        .build();
  }
}
