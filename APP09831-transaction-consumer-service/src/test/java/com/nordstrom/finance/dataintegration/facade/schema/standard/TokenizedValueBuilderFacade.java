package com.nordstrom.finance.dataintegration.facade.schema.standard;

import com.nordstrom.event.secure.DataClassification;
import com.nordstrom.event.secure.TokenizedValue;
import java.util.function.UnaryOperator;

public class TokenizedValueBuilderFacade {

  private TokenizedValueBuilderFacade() {}

  private static TokenizedValue.Builder getDefaultBuilder() {
    return TokenizedValue.newBuilder()
        .setValue("M3GgO3SDTtmEp-g6Se6pHg")
        .setAuthority("CONTRACT")
        .setDataClassification(DataClassification.SENSITIVE);
  }

  public static TokenizedValue build(UnaryOperator<TokenizedValue.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static TokenizedValue buildDefault() {
    return build(UnaryOperator.identity());
  }
}
