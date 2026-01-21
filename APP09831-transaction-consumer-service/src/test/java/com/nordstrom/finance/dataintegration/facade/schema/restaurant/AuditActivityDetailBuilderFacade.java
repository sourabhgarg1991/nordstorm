package com.nordstrom.finance.dataintegration.facade.schema.restaurant;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_INSTANT;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionAuditActivityDetail;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionAuditActivityType;
import com.nordstrom.event.rosettastone.Employee;
import com.nordstrom.event.rosettastone.EmployeeIdType;
import com.nordstrom.standard.User;
import com.nordstrom.standard.UserType;
import java.util.function.UnaryOperator;

public class AuditActivityDetailBuilderFacade {

  private AuditActivityDetailBuilderFacade() {}

  private static FinancialRestaurantTransactionAuditActivityDetail.Builder getDefaultBuilder() {
    return FinancialRestaurantTransactionAuditActivityDetail.newBuilder()
        .setAuditActivityType(FinancialRestaurantTransactionAuditActivityType.ERROR_CORRECTED)
        .setUpdatedTime(TESTING_INSTANT)
        .setNote("Notes")
        .setUser(
            User.newBuilder()
                .setSystemId("SId-1")
                .setEmployee(
                    Employee.newBuilder()
                        .setId("Id-1")
                        .setIdType(EmployeeIdType.EMPLOYEE_EMAIL)
                        .build())
                .setType(UserType.EMPLOYEE)
                .build());
  }

  public static FinancialRestaurantTransactionAuditActivityDetail build(
      UnaryOperator<FinancialRestaurantTransactionAuditActivityDetail.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRestaurantTransactionAuditActivityDetail buildDefault() {
    return build(UnaryOperator.identity());
  }
}
