package com.nordstrom.finance.dataintegration.facade.schema.restaurant;

import static java.util.stream.Collectors.toList;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionCardSubType;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionCardType;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionTender;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionTenderCaptureMethod;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionTenderType;
import com.nordstrom.event.rosettastone.Employee;
import com.nordstrom.event.rosettastone.EmployeeIdType;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.standard.MoneyV2;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class TenderDetailBuilderFacade {

  private TenderDetailBuilderFacade() {}

  public static FinancialRestaurantTransactionTender.Builder getDefaultBuilder() {
    return FinancialRestaurantTransactionTender.newBuilder()
        .setId("TId-1")
        .setTenderType(FinancialRestaurantTransactionTenderType.CREDIT_CARD)
        .setTotal(MoneyBuilderFacade.build(11, 0))
        .setCardType(FinancialRestaurantTransactionCardType.VISA)
        .setTenderCaptureMethod(FinancialRestaurantTransactionTenderCaptureMethod.INSERT)
        .setTipTotal(MoneyBuilderFacade.build(0, 0))
        .setEmployee(
            Employee.newBuilder().setId("EMP-1").setIdType(EmployeeIdType.EMPLOYEE_NUMBER).build());
  }

  public static FinancialRestaurantTransactionTender build(
      UnaryOperator<FinancialRestaurantTransactionTender.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRestaurantTransactionTender buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static List<FinancialRestaurantTransactionTender> buildDefaultList(int count) {
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> TenderDetailBuilderFacade.buildDefault())
        .collect(toList());
  }

  public static List<FinancialRestaurantTransactionTender> buildList(
      int count, UnaryOperator<FinancialRestaurantTransactionTender.Builder> modifier) {
    return IntStream.range(0, count).parallel().mapToObj(i -> build(modifier)).toList();
  }

  public static UnaryOperator<FinancialRestaurantTransactionTender.Builder>
      buildWithTenderTypesAndAmount(
          FinancialRestaurantTransactionTenderType tenderType,
          FinancialRestaurantTransactionCardType cardType,
          FinancialRestaurantTransactionCardSubType cardSubType,
          MoneyV2 total) {
    return builder ->
        builder
            .setTenderType(tenderType)
            .setCardType(cardType)
            .setCardSubType(cardSubType)
            .setTotal(total);
  }

  public static UnaryOperator<FinancialRestaurantTransactionTender.Builder>
      buildWithTenderTypesAmountAndTip(
          FinancialRestaurantTransactionTenderType tenderType,
          FinancialRestaurantTransactionCardType cardType,
          FinancialRestaurantTransactionCardSubType cardSubType,
          MoneyV2 total,
          MoneyV2 tip) {
    return builder ->
        builder
            .setTenderType(tenderType)
            .setCardType(cardType)
            .setCardSubType(cardSubType)
            .setTotal(total)
            .setTipTotal(tip);
  }
}
