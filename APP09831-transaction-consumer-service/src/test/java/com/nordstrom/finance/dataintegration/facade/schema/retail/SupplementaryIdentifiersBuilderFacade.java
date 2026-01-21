package com.nordstrom.finance.dataintegration.facade.schema.retail;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionSupplementaryIdentifiers;
import java.util.function.UnaryOperator;

public class SupplementaryIdentifiersBuilderFacade {

  private SupplementaryIdentifiersBuilderFacade() {}

  private static FinancialRetailTransactionSupplementaryIdentifiers.Builder getDefaultBuilder() {
    return FinancialRetailTransactionSupplementaryIdentifiers.newBuilder()
        .setStoreTransactionId("STId-1")
        .setOrderNumber("ON-1");
  }

  public static FinancialRetailTransactionSupplementaryIdentifiers build(
      UnaryOperator<FinancialRetailTransactionSupplementaryIdentifiers.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRetailTransactionSupplementaryIdentifiers buildDefault() {
    return build(UnaryOperator.identity());
  }
}
