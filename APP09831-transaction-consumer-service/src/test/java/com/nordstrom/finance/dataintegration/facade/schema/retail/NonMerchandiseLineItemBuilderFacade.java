package com.nordstrom.finance.dataintegration.facade.schema.retail;

import static com.nordstrom.customer.object.operational.FinancialRetailTransactionNonMerchandiseLineItemType.RETAIL_DELIVERY_FEE;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemTax;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionNonMerchandiseLineItem;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class NonMerchandiseLineItemBuilderFacade {

  private NonMerchandiseLineItemBuilderFacade() {}

  private static FinancialRetailTransactionNonMerchandiseLineItem.Builder getDefaultBuilder() {
    FinancialRetailTransactionItemTax itemTax =
        TransactionItemTaxBuilderFacade.build(
            tax -> tax.setTaxTotal(MoneyBuilderFacade.buildZero()));

    return FinancialRetailTransactionNonMerchandiseLineItem.newBuilder()
        .setTransactionLineId("item_1")
        .setSalePrice(MoneyBuilderFacade.build(10, 500_000_000))
        .setType(RETAIL_DELIVERY_FEE)
        .setFeeCode("FC-1")
        .setReturnDetail(null)
        .setTax(itemTax)
        .setIntentLocationId("ILId-1");
  }

  public static FinancialRetailTransactionNonMerchandiseLineItem build(
      UnaryOperator<FinancialRetailTransactionNonMerchandiseLineItem.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRetailTransactionNonMerchandiseLineItem buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static List<FinancialRetailTransactionNonMerchandiseLineItem> buildList(
      int count, UnaryOperator<FinancialRetailTransactionNonMerchandiseLineItem.Builder> modifier) {
    return IntStream.range(0, count).parallel().mapToObj(i -> build(modifier)).toList();
  }

  public static List<FinancialRetailTransactionNonMerchandiseLineItem> buildDefaultList(int count) {
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> build(it -> it.setTransactionLineId("TLId-" + i)))
        .toList();
  }

  // Custom builders:
  public static FinancialRetailTransactionNonMerchandiseLineItem buildWithDeliveryFee(
      long units, int nanos) {
    return build(item -> item.setSalePrice(MoneyBuilderFacade.build(units, nanos)));
  }
}
