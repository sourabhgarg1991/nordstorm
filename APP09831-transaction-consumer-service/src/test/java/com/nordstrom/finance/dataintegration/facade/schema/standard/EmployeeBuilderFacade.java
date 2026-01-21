package com.nordstrom.finance.dataintegration.facade.schema.standard;

import com.nordstrom.event.rosettastone.Employee;
import com.nordstrom.event.rosettastone.EmployeeIdType;
import java.util.function.UnaryOperator;

public class EmployeeBuilderFacade {
  private EmployeeBuilderFacade() {}

  private static Employee.Builder getDefaultBuilder() {
    return Employee.newBuilder().setId("Id-1").setIdType(EmployeeIdType.EMPLOYEE_EMAIL);
  }

  public static Employee build(UnaryOperator<Employee.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static Employee buildDefault() {
    return build(UnaryOperator.identity());
  }
}
