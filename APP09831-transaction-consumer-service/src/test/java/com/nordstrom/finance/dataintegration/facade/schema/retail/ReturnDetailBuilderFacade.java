package com.nordstrom.finance.dataintegration.facade.schema.retail;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_INSTANT;
import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_LOCAL_DATE;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemRefundAdjustment;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionItemRefundAdjustmentReason;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionMerchandiseItemReturnDetail;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionOriginalTransactionSupplementaryIdentifiers;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionPointOfSalePurchaseId;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionPromotion;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ReturnDetailBuilderFacade {

  private ReturnDetailBuilderFacade() {}

  private static FinancialRetailTransactionMerchandiseItemReturnDetail.Builder getDefaultBuilder() {
    return FinancialRetailTransactionMerchandiseItemReturnDetail.newBuilder()
        .setOriginalTransactionSupplementaryIdentifiers(
            FinancialRetailTransactionOriginalTransactionSupplementaryIdentifiers.newBuilder()
                .setOriginalFinancialRetailTransactionRecordId("OFRTRId-1")
                .setStoreTransactionId("STId-1")
                .setOriginalCustomerTransactionId("OCTId-1")
                .setPosPurchaseId(
                    FinancialRetailTransactionPointOfSalePurchaseId.newBuilder()
                        .setStore("S-1")
                        .setRegister("R-1")
                        .setTransactionNumber("TN-1")
                        .setBusinessDate(TESTING_LOCAL_DATE)
                        .build())
                .build())
        .setReturnMerchandiseAuthorizationId("RMAId-1")
        .setOriginalBusinessDate(TESTING_LOCAL_DATE)
        .setRefundAdjustment(null)
        .setOriginalSalePrice(MoneyBuilderFacade.buildDefault())
        .setOriginalOfferPrice(MoneyBuilderFacade.buildDefault())
        .setOriginalPrePromotionPrice(MoneyBuilderFacade.buildDefault())
        .setOriginalTransactionTime(TESTING_INSTANT)
        .setOriginalTax(TransactionItemTaxBuilderFacade.buildDefault())
        .setOriginalPromotions(
            List.of(
                FinancialRetailTransactionPromotion.newBuilder()
                    .setDiscount(MoneyBuilderFacade.buildDefault())
                    .setPromotionId("PId1")
                    .setPercentOff(0.01)
                    .setIsEmployeeDiscountApplied(true)
                    .build()))
        .setIsNoProductReturn(false)
        .setReturnLocationId("RLId-1")
        .setOriginalRetailLocationId("ORLId-1");
  }

  public static FinancialRetailTransactionMerchandiseItemReturnDetail build(
      UnaryOperator<FinancialRetailTransactionMerchandiseItemReturnDetail.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  @SafeVarargs
  public static FinancialRetailTransactionMerchandiseItemReturnDetail build(
      UnaryOperator<FinancialRetailTransactionMerchandiseItemReturnDetail.Builder>... modifiers) {
    UnaryOperator<FinancialRetailTransactionMerchandiseItemReturnDetail.Builder> combinedModifier =
        Stream.of(modifiers).reduce(UnaryOperator.identity(), (a, b) -> a.andThen(b)::apply);
    return build(combinedModifier);
  }

  public static FinancialRetailTransactionMerchandiseItemReturnDetail buildDefault() {
    return build(UnaryOperator.identity());
  }

  // Custom modifiers:
  public static UnaryOperator<FinancialRetailTransactionMerchandiseItemReturnDetail.Builder>
      withRefundAdjustment(
          FinancialRetailTransactionItemRefundAdjustmentReason refundAdjustmentReason) {
    FinancialRetailTransactionItemRefundAdjustment refundAdjustment =
        FinancialRetailTransactionItemRefundAdjustment.newBuilder()
            .setDeductedAmount(MoneyBuilderFacade.buildDefault())
            .setAdjustmentReason(refundAdjustmentReason)
            .build();
    return builder -> builder.setRefundAdjustment(refundAdjustment);
  }
}
