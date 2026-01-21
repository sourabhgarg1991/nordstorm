package com.nordstrom.finance.dataintegration.facade.schema.retail;

import static java.util.stream.Collectors.toList;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionTender;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionTenderDetail;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionTenderStatus;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionTenderType;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.standard.MoneyV2;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import org.springframework.data.util.Pair;

public class TenderDetailBuilderFacade {

  private TenderDetailBuilderFacade() {}

  private static FinancialRetailTransactionTenderDetail.Builder getDefaultBuilder() {
    return FinancialRetailTransactionTenderDetail.newBuilder()
        .setTotal(MoneyBuilderFacade.build(31, 500_000_000))
        .setTender(FinancialRetailTransactionTender.newBuilder().build())
        .setTenderType(FinancialRetailTransactionTenderType.CREDIT_CARD)
        .setStatus(FinancialRetailTransactionTenderStatus.SUCCEEDED);
  }

  public static FinancialRetailTransactionTenderDetail build(
      UnaryOperator<FinancialRetailTransactionTenderDetail.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRetailTransactionTenderDetail buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static List<FinancialRetailTransactionTenderDetail> buildDefaultList(int count) {
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> TenderDetailBuilderFacade.buildDefault())
        .collect(toList());
  }

  public static List<FinancialRetailTransactionTenderDetail> buildList(
      int count, UnaryOperator<FinancialRetailTransactionTenderDetail.Builder> modifier) {
    return IntStream.range(0, count).parallel().mapToObj(i -> build(modifier)).toList();
  }

  // Custom builders:
  public static List<FinancialRetailTransactionTenderDetail> buildWithTenderTypes(
      List<Pair<FinancialRetailTransactionTenderType, MoneyV2>> tenders) {
    return tenders.stream()
        .map(
            type ->
                build(
                    tender -> {
                      tender.setTenderType(type.getFirst());
                      tender.setTotal(type.getSecond());
                      return tender;
                    }))
        .toList();
  }
}
