package com.nordstrom.finance.dataintegration.facade.schema.retail;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionAdjustment;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionAdjustmentReason;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionAdjustmentType;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import java.util.function.UnaryOperator;

public class TransactionAdjustmentBuilderFacade {

  private TransactionAdjustmentBuilderFacade() {}

  private static FinancialRetailTransactionAdjustment.Builder getDefaultBuilder() {
    return FinancialRetailTransactionAdjustment.newBuilder()
        .setAdjustmentType(FinancialRetailTransactionAdjustmentType.CREDIT)
        .setReason(FinancialRetailTransactionAdjustmentReason.OUTSIDE_OF_RETURN_POLICY)
        .setAmount(MoneyBuilderFacade.buildDefault());
  }

  public static FinancialRetailTransactionAdjustment build(
      UnaryOperator<FinancialRetailTransactionAdjustment.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRetailTransactionAdjustment buildDefault() {
    return build(UnaryOperator.identity());
  }
}
