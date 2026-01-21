package com.nordstrom.finance.dataintegration.facade.schema.retail;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionCommissionSource;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionDepartmentClassSource;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemDepartmentAndClass;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemEmployeeCommissionDetail;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemRefundAdjustmentReason;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionMerchandiseItemReturnDetail;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionMerchandiseLineItem;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionRevenueRecognizedDetail;
import com.nordstrom.event.secure.TokenizedValue;
import com.nordstrom.finance.dataintegration.facade.schema.standard.EmployeeBuilderFacade;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.finance.dataintegration.facade.schema.standard.TokenizedMoneyBuilderFacade;
import com.nordstrom.finance.dataintegration.facade.schema.standard.TokenizedValueBuilderFacade;
import com.nordstrom.standard.MoneyV2;
import com.nordstrom.standard.TokenizedMoneyV2;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MerchandiseLineItemBuilderFacade {

  private MerchandiseLineItemBuilderFacade() {}

  private static FinancialRetailTransactionMerchandiseLineItem.Builder getDefaultBuilder() {
    return FinancialRetailTransactionMerchandiseLineItem.newBuilder()
        .setTransactionLineId("TLId-1")
        .setOfferPrice(MoneyBuilderFacade.buildDefault())
        .setSalePrice(MoneyBuilderFacade.buildDefault())
        .setSkuId("SId-1")
        .setUpc("UPC-1")
        .setIsFinalSale(true)
        .setPromotions(Collections.emptyList())
        .setRevenueRecognizedDetail(
            FinancialRetailTransactionRevenueRecognizedDetail.newBuilder()
                .setRevenueRecognized(TokenizedMoneyBuilderFacade.buildDefault())
                .setTaxOnRevenueRecognized(TokenizedMoneyBuilderFacade.buildDefault())
                .build())
        .setTax(TransactionItemTaxBuilderFacade.buildDefault())
        .setQuantity(1)
        .setDepartmentClass(
            FinancialRetailTransactionItemDepartmentAndClass.newBuilder()
                .setClassNumber("CN-1")
                .setDepartmentNumber("DN-1")
                .setDepartmentClassSource(FinancialRetailTransactionDepartmentClassSource.MANUAL)
                .build())
        .setIntentLocationId("ILId-1")
        .setFulfillingLocationId("FLId-1")
        .setEmployeeCommissionDetail(
            FinancialRetailTransactionItemEmployeeCommissionDetail.newBuilder()
                .setEmployee(EmployeeBuilderFacade.buildDefault())
                .setCommissionSource(FinancialRetailTransactionCommissionSource.POINT_OF_SALE)
                .setStyleBoardId("SBId-1")
                .setLookId("LId-1")
                .build())
        .setReturnDetail(null);
  }

  public static FinancialRetailTransactionMerchandiseLineItem build(
      UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  @SafeVarargs
  public static FinancialRetailTransactionMerchandiseLineItem build(
      UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder>... modifiers) {
    UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder> combinedModifier =
        Stream.of(modifiers).reduce(UnaryOperator.identity(), (a, b) -> a.andThen(b)::apply);
    return build(combinedModifier);
  }

  public static FinancialRetailTransactionMerchandiseLineItem buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem> buildList(
      int count, UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder> modifier) {
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> build(modifier, item -> item.setTransactionLineId("TLId-" + i)))
        .toList();
  }

  @SafeVarargs
  public static List<FinancialRetailTransactionMerchandiseLineItem> buildList(
      int count,
      UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder>... modifiers) {
    UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder> combinedModifier =
        Stream.of(modifiers).reduce(UnaryOperator.identity(), (a, b) -> a.andThen(b)::apply);
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> build(combinedModifier, item -> item.setTransactionLineId("TLId-" + i)))
        .toList();
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem> buildDefaultList(int count) {
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> build(item -> item.setTransactionLineId("TLId-" + i)))
        .toList();
  }

  @SafeVarargs
  public static List<FinancialRetailTransactionMerchandiseLineItem> buildCompositeList(
      List<FinancialRetailTransactionMerchandiseLineItem>... lists) {
    return Arrays.stream(lists).flatMap(List::stream).toList();
  }

  // Custom modifiers:
  public static UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder>
      withEmployeeCommissionDetail() {
    return builder ->
        builder.setEmployeeCommissionDetail(EmployeeCommissionDetailBuilderFacade.buildDefault());
  }

  public static UnaryOperator<FinancialRetailTransactionMerchandiseLineItem.Builder>
      withoutEmployeeCommissionDetail() {
    return builder -> builder.setEmployeeCommissionDetail(null);
  }

  // Custom builders:
  public static List<FinancialRetailTransactionMerchandiseLineItem>
      buildCommissionAndNonCommissionItems(int commissionItemsCount, int nonCommissionItemsCount) {
    List<FinancialRetailTransactionMerchandiseLineItem> commissionMerchandiseLineItems =
        buildList(commissionItemsCount, withEmployeeCommissionDetail());

    List<FinancialRetailTransactionMerchandiseLineItem> nonCommissionMerchandiseLineItems =
        buildList(nonCommissionItemsCount, withoutEmployeeCommissionDetail());

    return buildCompositeList(commissionMerchandiseLineItems, nonCommissionMerchandiseLineItems);
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem>
      buildCommissionAndNonCommissionItemsReturn(
          int commissionItemsCount, int nonCommissionItemsCount) {
    List<FinancialRetailTransactionMerchandiseLineItem> commissionMerchandiseLineItems =
        buildList(
            commissionItemsCount,
            withEmployeeCommissionDetail(),
            item -> {
              item.setSalePrice(MoneyBuilderFacade.build(-100, -500_000_000));
              item.setReturnDetail(ReturnDetailBuilderFacade.buildDefault());
              item.setTax(
                  TransactionItemTaxBuilderFacade.build(
                      taxBuilder -> {
                        taxBuilder.setTaxTotal(MoneyBuilderFacade.build(-1, -500_000_000));
                        return taxBuilder;
                      }));
              return item;
            });

    List<FinancialRetailTransactionMerchandiseLineItem> nonCommissionMerchandiseLineItems =
        buildList(
            nonCommissionItemsCount,
            withoutEmployeeCommissionDetail(),
            item -> {
              item.setSalePrice(MoneyBuilderFacade.build(-100, -500_000_000));
              item.setReturnDetail(ReturnDetailBuilderFacade.buildDefault());
              item.setTax(
                  TransactionItemTaxBuilderFacade.build(
                      taxBuilder -> {
                        taxBuilder.setTaxTotal(MoneyBuilderFacade.build(-1, -500_000_000));
                        return taxBuilder;
                      }));
              return item;
            });

    return buildCompositeList(commissionMerchandiseLineItems, nonCommissionMerchandiseLineItems);
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem> buildWithItemsReturnDetails(
      int count, FinancialRetailTransactionItemRefundAdjustmentReason refundAdjustment) {
    FinancialRetailTransactionMerchandiseItemReturnDetail returnDetail =
        ReturnDetailBuilderFacade.build(
            ReturnDetailBuilderFacade.withRefundAdjustment(refundAdjustment));

    return buildList(
        count,
        item -> {
          item.setReturnDetail(returnDetail);
          item.setSalePrice(MoneyBuilderFacade.build(-100, -500_000_000));
          item.setTax(
              TransactionItemTaxBuilderFacade.build(
                  taxBuilder -> {
                    taxBuilder.setTaxTotal(MoneyBuilderFacade.build(-1, -500_000_000));
                    return taxBuilder;
                  }));
          return item;
        });
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem> buildWithCorruptedTokens() {
    TokenizedValue corruptedToken =
        TokenizedValueBuilderFacade.build(token -> token.setValue("units"));
    TokenizedMoneyV2 corruptedTokenizedMoney =
        TokenizedMoneyBuilderFacade.build(
            money -> money.setUnits(corruptedToken).setNanos(corruptedToken));

    return buildList(
        1,
        item -> {
          item.setRevenueRecognizedDetail(
              FinancialRetailTransactionRevenueRecognizedDetail.newBuilder()
                  .setRevenueRecognized(corruptedTokenizedMoney)
                  .setTaxOnRevenueRecognized(corruptedTokenizedMoney)
                  .build());
          return item;
        });
  }

  // mock data for scenarios documented in
  // https://confluence.nordstrom.com/pages/viewpage.action?pageId=1498461698
  private static FinancialRetailTransactionMerchandiseLineItem buildScenarioItem(
      String scenarioSuffix,
      MoneyV2 price,
      TokenizedMoneyV2 revenueRecognized,
      MoneyV2 tax,
      FinancialRetailTransactionMerchandiseItemReturnDetail returnDetail) {
    return FinancialRetailTransactionMerchandiseLineItem.newBuilder()
        .setTransactionLineId("item_" + scenarioSuffix)
        .setOfferPrice(price)
        .setSalePrice(price)
        .setSkuId("SId-1")
        .setUpc("UPC-1")
        .setIsFinalSale(true)
        .setPromotions(Collections.emptyList())
        .setRevenueRecognizedDetail(
            FinancialRetailTransactionRevenueRecognizedDetail.newBuilder()
                .setRevenueRecognized(revenueRecognized)
                .setTaxOnRevenueRecognized(TokenizedMoneyBuilderFacade.buildDefault())
                .build())
        .setTax(
            TransactionItemTaxBuilderFacade.build(
                TransactionItemTaxBuilderFacade.withTaxAmount(tax)))
        .setQuantity(1)
        .setDepartmentClass(
            FinancialRetailTransactionItemDepartmentAndClass.newBuilder()
                .setClassNumber("CN-1")
                .setDepartmentNumber("DN-1")
                .setDepartmentClassSource(FinancialRetailTransactionDepartmentClassSource.MANUAL)
                .build())
        .setIntentLocationId("ILId-1")
        .setFulfillingLocationId("FLId-1")
        .setEmployeeCommissionDetail(
            FinancialRetailTransactionItemEmployeeCommissionDetail.newBuilder()
                .setEmployee(EmployeeBuilderFacade.buildDefault())
                .setCommissionSource(FinancialRetailTransactionCommissionSource.POINT_OF_SALE)
                .setStyleBoardId("SBId-1")
                .setLookId("LId-1")
                .build())
        .setReturnDetail(returnDetail)
        .build();
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem> singleItemScenario() {
    return List.of(
        buildScenarioItem(
            "1",
            MoneyBuilderFacade.build(100, 0),
            TokenizedMoneyBuilderFacade.buildRevRecognizedItem1(),
            MoneyBuilderFacade.build(10, 0),
            null));
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem> twoItemScenario() {
    return List.of(
        buildScenarioItem(
            "1",
            MoneyBuilderFacade.build(100, 0),
            TokenizedMoneyBuilderFacade.buildRevRecognizedItem1(),
            MoneyBuilderFacade.build(10, 0),
            null),
        buildScenarioItem(
            "2",
            MoneyBuilderFacade.build(50, 0),
            TokenizedMoneyBuilderFacade.buildRevRecognizedItem2(),
            MoneyBuilderFacade.build(5, 0),
            null));
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem>
      outOfPolicySingleItemScenario() {
    return List.of(
        buildScenarioItem(
            "1",
            MoneyBuilderFacade.build(100, 0),
            TokenizedMoneyBuilderFacade.buildRevRecognizedItem1(),
            MoneyBuilderFacade.build(10, 0),
            ReturnDetailBuilderFacade.build(
                ReturnDetailBuilderFacade.withRefundAdjustment(
                    FinancialRetailTransactionItemRefundAdjustmentReason
                        .OVERRIDE_ON_RETURN_POLICY))));
  }

  public static List<FinancialRetailTransactionMerchandiseLineItem> outOfPolicyTwoItemScenario() {
    return List.of(
        buildScenarioItem(
            "1",
            MoneyBuilderFacade.build(100, 0),
            TokenizedMoneyBuilderFacade.buildRevRecognizedItem1(),
            MoneyBuilderFacade.build(10, 0),
            ReturnDetailBuilderFacade.buildDefault()),
        buildScenarioItem(
            "2",
            MoneyBuilderFacade.build(50, 0),
            TokenizedMoneyBuilderFacade.buildRevRecognizedItem2(),
            MoneyBuilderFacade.build(5, 0),
            ReturnDetailBuilderFacade.build(
                ReturnDetailBuilderFacade.withRefundAdjustment(
                    FinancialRetailTransactionItemRefundAdjustmentReason
                        .OVERRIDE_ON_RETURN_POLICY))));
  }
}
