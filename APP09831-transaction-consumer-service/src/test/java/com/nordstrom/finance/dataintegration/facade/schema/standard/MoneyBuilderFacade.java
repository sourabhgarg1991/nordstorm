package com.nordstrom.finance.dataintegration.facade.schema.standard;

import com.nordstrom.standard.CurrencyCodeV2;
import com.nordstrom.standard.MoneyV2;
import java.util.function.UnaryOperator;

public class MoneyBuilderFacade {

  private MoneyBuilderFacade() {}

  private static MoneyV2.Builder getDefaultBuilder() {
    return MoneyV2.newBuilder()
        .setCurrencyCode(CurrencyCodeV2.USD)
        .setUnits(100)
        .setNanos(500_000_000);
  }

  public static MoneyV2 build(UnaryOperator<MoneyV2.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static MoneyV2 buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static MoneyV2 build(long units, int nanos) {
    return build(
        money -> {
          money.setUnits(units);
          money.setNanos(nanos);
          return money;
        });
  }

  public static MoneyV2 buildZero() {
    return build(0, 0);
  }

  public static MoneyV2 buildOpposite(MoneyV2 value) {
    return build(-1 * value.getUnits(), -1 * value.getNanos());
  }
}
