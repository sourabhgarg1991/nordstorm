package com.nordstrom.finance.dataintegration.facade.schema.retail;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_INSTANT;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionAuditActivityDetail;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionAuditActivityType;
import com.nordstrom.finance.dataintegration.facade.schema.standard.EmployeeBuilderFacade;
import com.nordstrom.standard.User;
import com.nordstrom.standard.UserType;
import java.util.function.UnaryOperator;

public class AuditActivityDetailBuilderFacade {

  private AuditActivityDetailBuilderFacade() {}

  private static FinancialRetailTransactionAuditActivityDetail.Builder getDefaultBuilder() {
    return FinancialRetailTransactionAuditActivityDetail.newBuilder()
        .setAuditActivityType(FinancialRetailTransactionAuditActivityType.ERROR_CORRECTED)
        .setUpdatedTime(TESTING_INSTANT)
        .setNote("Notes")
        .setUser(
            User.newBuilder()
                .setSystemId("SId-1")
                .setEmployee(EmployeeBuilderFacade.buildDefault())
                .setType(UserType.EMPLOYEE)
                .build());
  }

  public static FinancialRetailTransactionAuditActivityDetail build(
      UnaryOperator<FinancialRetailTransactionAuditActivityDetail.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRetailTransactionAuditActivityDetail buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static FinancialRetailTransactionAuditActivityDetail withCleanDeletedAuditActivity() {
    return getDefaultBuilder()
        .setAuditActivityType(FinancialRetailTransactionAuditActivityType.CLEAN_DELETED)
        .build();
  }
}
