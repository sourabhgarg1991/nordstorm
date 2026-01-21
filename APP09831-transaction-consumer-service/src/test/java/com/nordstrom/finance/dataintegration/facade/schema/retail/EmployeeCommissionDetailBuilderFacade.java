package com.nordstrom.finance.dataintegration.facade.schema.retail;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionCommissionSource;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemEmployeeCommissionDetail;
import com.nordstrom.finance.dataintegration.facade.schema.standard.EmployeeBuilderFacade;
import java.util.function.UnaryOperator;

public class EmployeeCommissionDetailBuilderFacade {
  private EmployeeCommissionDetailBuilderFacade() {}

  private static FinancialRetailTransactionItemEmployeeCommissionDetail.Builder
      getDefaultBuilder() {
    return FinancialRetailTransactionItemEmployeeCommissionDetail.newBuilder()
        .setEmployee(EmployeeBuilderFacade.buildDefault())
        .setCommissionSource(FinancialRetailTransactionCommissionSource.POINT_OF_SALE)
        .setStyleBoardId("SBId-1")
        .setLookId("LId-1");
  }

  public static FinancialRetailTransactionItemEmployeeCommissionDetail build(
      UnaryOperator<FinancialRetailTransactionItemEmployeeCommissionDetail.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRetailTransactionItemEmployeeCommissionDetail buildDefault() {
    return build(UnaryOperator.identity());
  }
}
