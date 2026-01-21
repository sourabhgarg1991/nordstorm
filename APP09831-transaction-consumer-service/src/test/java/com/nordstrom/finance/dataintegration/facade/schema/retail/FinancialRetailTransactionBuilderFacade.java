package com.nordstrom.finance.dataintegration.facade.schema.retail;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_INSTANT;
import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_LOCAL_DATE;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.ValidationResultBuilderFacade.withFailureDetail;

import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionCustomer;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionMerchandiseLineItem;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionRetailBrand;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionRetailCountry;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionSourceExperience;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionType;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.finance.dataintegration.facade.schema.standard.PartnerRelationshipBuilderFacade;
import com.nordstrom.standard.MoneyV2;
import com.nordstrom.standard.PartnerRelationship;
import com.nordstrom.standard.PartnerRelationshipType;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class FinancialRetailTransactionBuilderFacade {

  // Private constructor to prevent instantiation
  private FinancialRetailTransactionBuilderFacade() {}

  // Default builder
  private static FinancialRetailTransaction.Builder getDefaultBuilder() {
    MoneyV2 defaultMoney = MoneyBuilderFacade.buildDefault();

    return FinancialRetailTransaction.newBuilder()
        .setCustomerTransactionId("TransactionId-1")
        .setIsReversed(false)
        .setFinancialRetailTransactionRecordId("SdmId-1")
        .setTransactionType(FinancialRetailTransactionType.SALE)
        .setBusinessDate(TESTING_LOCAL_DATE)
        .setSupplementaryIdentifiers(SupplementaryIdentifiersBuilderFacade.buildDefault())
        .setCreationTime(TESTING_INSTANT)
        .setTransactionTime(TESTING_INSTANT)
        .setRetailCountry(FinancialRetailTransactionRetailCountry.US)
        .setRetailBrand(FinancialRetailTransactionRetailBrand.NORDSTROM)
        .setSourceExperience(FinancialRetailTransactionSourceExperience.GLOBAL_STORE)
        .setAuditActivityDetail(AuditActivityDetailBuilderFacade.buildDefault())
        .setCustomer(FinancialRetailTransactionCustomer.newBuilder().build())
        .setValidationResult(ValidationResultBuilderFacade.buildDefault())
        .setTaxTotal(MoneyBuilderFacade.build(10, 500_000_000))
        .setTotal(defaultMoney)
        .setMerchandiseLineItems(MerchandiseLineItemBuilderFacade.buildDefaultList(3))
        .setNonMerchandiseLineItems(
            Collections.singletonList(NonMerchandiseLineItemBuilderFacade.buildDefault()))
        .setPromotionDiscountEmployee(null)
        .setTenderDetails(TenderDetailBuilderFacade.buildDefaultList(1))
        .setPartnerRelationship(
            PartnerRelationship.newBuilder()
                .setId("Id-1")
                .setType(PartnerRelationshipType.ECONCESSION)
                .build())
        .setVersionNumber("1")
        .setDiscountTotal(defaultMoney)
        .setRetailLocationId("RLId-1");
  }

  // Build with single modifier
  public static FinancialRetailTransaction build(
      UnaryOperator<FinancialRetailTransaction.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  // Build with multiple modifiers
  @SafeVarargs
  public static FinancialRetailTransaction build(
      UnaryOperator<FinancialRetailTransaction.Builder>... modifiers) {
    UnaryOperator<FinancialRetailTransaction.Builder> combinedModifier =
        Stream.of(modifiers).reduce(UnaryOperator.identity(), (a, b) -> a.andThen(b)::apply);
    return build(combinedModifier);
  }

  // Build with default values
  public static FinancialRetailTransaction buildDefault() {
    return build(UnaryOperator.identity());
  }

  // Custom modifiers:
  public static UnaryOperator<FinancialRetailTransaction.Builder> withReturnTypeDefault() {
    return builder -> {
      builder.setTransactionType(FinancialRetailTransactionType.RETURN);
      builder.setTotal(MoneyBuilderFacade.buildOpposite(getDefaultBuilder().getTotal()));
      builder.setTaxTotal(MoneyBuilderFacade.buildOpposite(getDefaultBuilder().getTaxTotal()));

      builder.setMerchandiseLineItems(
          MerchandiseLineItemBuilderFacade.buildList(
              3,
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
              }));
      builder.setNonMerchandiseLineItems(
          Collections.singletonList(
              NonMerchandiseLineItemBuilderFacade.build(
                  item -> item.setSalePrice(MoneyBuilderFacade.build(-10, -500_000_000)))));
      builder.setTenderDetails(
          TenderDetailBuilderFacade.buildList(
              1, tender -> tender.setTotal(MoneyBuilderFacade.build(-31, -500_000_000))));

      return builder;
    };
  }

  public static UnaryOperator<FinancialRetailTransaction.Builder> withPartnerRelationship(
      PartnerRelationshipType partnerRelationshipType) {
    PartnerRelationship partnerRelationship =
        partnerRelationshipType == null
            ? null
            : PartnerRelationshipBuilderFacade.build(
                relationship -> relationship.setType(partnerRelationshipType));
    return builder -> builder.setPartnerRelationship(partnerRelationship);
  }

  public static UnaryOperator<FinancialRetailTransaction.Builder> withMerchandiseLineItems(
      List<FinancialRetailTransactionMerchandiseLineItem> items) {
    return builder -> builder.setMerchandiseLineItems(items);
  }

  public static UnaryOperator<FinancialRetailTransaction.Builder> withFailedValidation() {
    return builder ->
        builder.setValidationResult(ValidationResultBuilderFacade.build(withFailureDetail()));
  }

  public static UnaryOperator<FinancialRetailTransaction.Builder> withCleanDeletedAuditActivity() {
    return builder ->
        builder.setAuditActivityDetail(
            AuditActivityDetailBuilderFacade.withCleanDeletedAuditActivity());
  }

  public static UnaryOperator<FinancialRetailTransaction.Builder> withZeroTotal() {
    return builder -> builder.setTotal(MoneyBuilderFacade.buildZero());
  }

  public static UnaryOperator<FinancialRetailTransaction.Builder> withTransactionAdjustment() {
    return builder ->
        builder.setTransactionAdjustment(TransactionAdjustmentBuilderFacade.buildDefault());
  }

  public static UnaryOperator<FinancialRetailTransaction.Builder>
      withoutItemsRevenueRecognizedDetail(int count) {
    return builder ->
        builder.setMerchandiseLineItems(
            MerchandiseLineItemBuilderFacade.buildList(
                count, item -> item.setRevenueRecognizedDetail(null)));
  }
}
