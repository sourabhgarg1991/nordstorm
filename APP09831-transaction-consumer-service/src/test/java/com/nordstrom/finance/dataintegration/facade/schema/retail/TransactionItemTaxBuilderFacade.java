package com.nordstrom.finance.dataintegration.facade.schema.retail;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemTax;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.standard.MoneyV2;
import java.util.function.UnaryOperator;

public class TransactionItemTaxBuilderFacade {

  private TransactionItemTaxBuilderFacade() {}

  private static FinancialRetailTransactionItemTax.Builder getDefaultBuilder() {
    return FinancialRetailTransactionItemTax.newBuilder()
        .setTaxTotal(MoneyBuilderFacade.build(1, 500_000_000))
        .setShipFromTaxAreaId("SFTAId-1")
        .setShipToTaxAreaId("STTAId-1")
        .setOriginTaxAreaId("OTAId-1")
        .setProductTaxCategoryCode("PTCC-1");
  }

  public static FinancialRetailTransactionItemTax build(
      UnaryOperator<FinancialRetailTransactionItemTax.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRetailTransactionItemTax buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static UnaryOperator<FinancialRetailTransactionItemTax.Builder> withTaxAmount(
      MoneyV2 amount) {
    return builder -> {
      builder.setTaxTotal(amount);
      return builder;
    };
  }
}
