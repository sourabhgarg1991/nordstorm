package com.nordstrom.finance.dataintegration.facade.schema.restaurant;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.DEPARTMENT_DEFAULT;
import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.ITEM_CLASS_DEFAULT;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionDepartmentClassSource;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionDiscount;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionDiscountType;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionItemDepartmentAndClass;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionMenuLineItem;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.standard.MoneyV2;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MenuItemBuilderFacade {

  private MenuItemBuilderFacade() {}

  private static FinancialRestaurantTransactionMenuLineItem.Builder getDefaultBuilder() {
    return FinancialRestaurantTransactionMenuLineItem.newBuilder()
        .setTransactionLineId("MI-1")
        .setPlu("PLU-1")
        .setRegularPrice(MoneyBuilderFacade.buildDefault())
        .setSalePrice(MoneyBuilderFacade.buildDefault())
        .setSkuId("SkuId-1")
        .setQuantity(1)
        .setDiscounts(Collections.emptyList())
        .setTotalTax(MoneyBuilderFacade.build(1, 0))
        .setItemDepartmentAndClass(
            FinancialRestaurantTransactionItemDepartmentAndClass.newBuilder()
                .setDepartmentNumber(DEPARTMENT_DEFAULT)
                .setClassNumber(ITEM_CLASS_DEFAULT)
                .setDepartmentClassSource(
                    FinancialRestaurantTransactionDepartmentClassSource.MAP_FILE)
                .build())
        .setSalesCategory("SC-1")
        .setPriceAfterPromosBeforeEmployeeDiscount(MoneyBuilderFacade.buildDefault());
  }

  public static FinancialRestaurantTransactionMenuLineItem build(
      UnaryOperator<FinancialRestaurantTransactionMenuLineItem.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  @SafeVarargs
  public static FinancialRestaurantTransactionMenuLineItem build(
      UnaryOperator<FinancialRestaurantTransactionMenuLineItem.Builder>... modifiers) {
    UnaryOperator<FinancialRestaurantTransactionMenuLineItem.Builder> combinedModifier =
        Stream.of(modifiers).reduce(UnaryOperator.identity(), (a, b) -> a.andThen(b)::apply);
    return build(combinedModifier);
  }

  public static UnaryOperator<FinancialRestaurantTransactionMenuLineItem.Builder>
      withIdAmountsDeptAndClass(
          String transactionId,
          MoneyV2 priceAfterPromosBeforeEmployeeDiscount,
          MoneyV2 totalTax,
          MoneyV2 employeeDiscount,
          String dept,
          String itemClass) {
    return builder ->
        builder
            .setTransactionLineId(transactionId)
            .setPriceAfterPromosBeforeEmployeeDiscount(priceAfterPromosBeforeEmployeeDiscount)
            .setTotalTax(totalTax)
            .setDiscounts(getFinancialRestaurantTransactionDiscount(employeeDiscount))
            .setItemDepartmentAndClass(
                FinancialRestaurantTransactionItemDepartmentAndClass.newBuilder()
                    .setDepartmentNumber(dept)
                    .setClassNumber(itemClass)
                    .setDepartmentClassSource(
                        FinancialRestaurantTransactionDepartmentClassSource.MAP_FILE)
                    .build());
  }

  public static UnaryOperator<FinancialRestaurantTransactionMenuLineItem.Builder>
      withDiscountDefault() {
    return builder ->
        builder.setDiscounts(
            getFinancialRestaurantTransactionDiscount(MoneyBuilderFacade.build(2, 0)));
  }

  public static FinancialRestaurantTransactionMenuLineItem buildDefault() {
    return build(UnaryOperator.identity());
  }

  public static List<FinancialRestaurantTransactionMenuLineItem> buildList(
      int count, UnaryOperator<FinancialRestaurantTransactionMenuLineItem.Builder> modifier) {
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> build(modifier, item -> item.setTransactionLineId("MI-" + i)))
        .toList();
  }

  public static List<FinancialRestaurantTransactionMenuLineItem> buildDefaultList(int count) {
    return IntStream.range(0, count)
        .parallel()
        .mapToObj(i -> build(item -> item.setTransactionLineId("MI-" + i)))
        .toList();
  }

  private static List<FinancialRestaurantTransactionDiscount>
      getFinancialRestaurantTransactionDiscount(MoneyV2 money) {
    return List.of(
        FinancialRestaurantTransactionDiscount.newBuilder()
            .setDiscountAmount(money)
            .setDiscountType(FinancialRestaurantTransactionDiscountType.EMPLOYEE_DISCOUNT)
            .setPromotionId("PROMO-1")
            .build());
  }
}
