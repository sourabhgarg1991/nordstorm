package com.nordstrom.finance.dataintegration.facade.entity;

import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_SYSTEM_TYPE_SDM;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_MARKETPLACE;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_RESTAURANT;
import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_LOCAL_DATE;

import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import java.util.Set;
import java.util.function.UnaryOperator;

public class TransactionFacade {
  private TransactionFacade() {}

  private static Transaction.TransactionBuilder getDefaultBuilder() {
    return Transaction.builder();
  }

  public static Transaction build(UnaryOperator<Transaction.TransactionBuilder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static Transaction buildMockMarketPlaceScenario(
      String scenarioSuffix, String transactionType, boolean isReversed) {
    return build(
        builder ->
            builder
                .sourceReferenceTransactionId("SdmId-" + scenarioSuffix)
                .sourceReferenceSystemType(SOURCE_REFERENCE_SYSTEM_TYPE_SDM)
                .sourceReferenceType(SOURCE_REFERENCE_TYPE_MARKETPLACE)
                .transactionType(transactionType)
                .businessDate(TESTING_LOCAL_DATE)
                .sourceProcessedDate(TESTING_LOCAL_DATE)
                .transactionDate(TESTING_LOCAL_DATE)
                .transactionReversalCode(isReversed ? "Y" : "N")
                .partnerRelationshipType("ECONCESSION")
                .createdDateTime(null)
                .lastUpdatedDateTime(null));
  }

  public static Transaction buildMockRestaurantScenario(
      String scenarioSuffix, String transactionType, boolean isReversed) {
    return build(
        builder ->
            builder
                .sourceReferenceTransactionId("SdmId-" + scenarioSuffix)
                .sourceReferenceSystemType(SOURCE_REFERENCE_SYSTEM_TYPE_SDM)
                .sourceReferenceType(SOURCE_REFERENCE_TYPE_RESTAURANT)
                .transactionType(transactionType)
                .businessDate(TESTING_LOCAL_DATE)
                .sourceProcessedDate(TESTING_LOCAL_DATE)
                .transactionDate(TESTING_LOCAL_DATE)
                .transactionReversalCode(isReversed ? "Y" : "N")
                .partnerRelationshipType(null)
                .createdDateTime(null)
                .lastUpdatedDateTime(null));
  }

  public static Transaction marketplaceSale() {
    Transaction finRetailTransactionAccounting = buildMockMarketPlaceScenario("1", "SALE", false);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockMarketPlaceLine("item_1", "FC-1", "10.50", "0.00", null),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-0", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-1", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-2", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceTender(
                "CREDIT_CARD", "CREDIT_CARD", "31.50")));
    return finRetailTransactionAccounting;
  }

  public static Transaction marketplaceReturn() {
    Transaction finRetailTransactionAccounting = buildMockMarketPlaceScenario("1", "RETURN", false);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockMarketPlaceLine("item_1", "FC-1", "10.50", "0.00", null),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-0", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-1", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-2", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceTender(
                "CREDIT_CARD", "CREDIT_CARD", "31.50")));
    return finRetailTransactionAccounting;
  }

  public static Transaction marketplaceSaleReversed() {
    Transaction finRetailTransactionAccounting = buildMockMarketPlaceScenario("1", "SALE", true);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockMarketPlaceLine("item_1", "FC-1", "10.50", "0.00", null),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-0", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-1", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-2", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceTender(
                "CREDIT_CARD", "CREDIT_CARD", "31.50")));
    return finRetailTransactionAccounting;
  }

  public static Transaction marketplaceReturnReversed() {
    Transaction finRetailTransactionAccounting = buildMockMarketPlaceScenario("1", "RETURN", true);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockMarketPlaceLine("item_1", "FC-1", "10.50", "0.00", null),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-0", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-1", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-2", null, "100.50", "1.50", "1"),
            TransactionLineFacade.buildMockMarketPlaceTender(
                "CREDIT_CARD", "CREDIT_CARD", "31.50")));
    return finRetailTransactionAccounting;
  }

  public static Transaction marketplaceWithoutCommission() {
    Transaction finRetailTransactionAccounting = buildMockMarketPlaceScenario("1", "SALE", false);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockMarketPlaceLine("item_1", "FC-1", "10.50", "0.00", null),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-0", null, "100.50", "1.50", null),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-1", null, "100.50", "1.50", null),
            TransactionLineFacade.buildMockMarketPlaceLine("TLId-2", null, "100.50", "1.50", null),
            TransactionLineFacade.buildMockMarketPlaceTender(
                "CREDIT_CARD", "CREDIT_CARD", "31.50")));
    return finRetailTransactionAccounting;
  }

  public static Transaction saleWithSingleItemAndSingleTender() {
    Transaction finRetailTransactionAccounting = buildMockRestaurantScenario("1", "SALE", false);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockRestaurantLine("MI-0", "100.00", "10.00", "0.00"),
            TransactionLineFacade.buildMockRestaurantTender(
                "TId-1", "CREDIT_CARD", "AMERICAN_EXPRESS", null, "110.0", "0.0")));
    return finRetailTransactionAccounting;
  }

  public static Transaction returnWithSingleItemAndSingleTender() {
    Transaction finRetailTransactionAccounting = buildMockRestaurantScenario("5", "RETURN", false);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockRestaurantLine("MI-0", "100.00", "10.00", "0.00"),
            TransactionLineFacade.buildMockRestaurantTender(
                "TId-1", "CREDIT_CARD", "AMERICAN_EXPRESS", null, "110.0", "0.0")));
    return finRetailTransactionAccounting;
  }

  public static Transaction reversedSaleWithSingleItemAndSingleTender() {
    Transaction finRetailTransactionAccounting = buildMockRestaurantScenario("2", "SALE", true);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockRestaurantLine("MI-0", "100.00", "10.00", "0.00"),
            TransactionLineFacade.buildMockRestaurantTender(
                "TId-1", "CREDIT_CARD", "AMERICAN_EXPRESS", null, "110.0", "0.0")));
    return finRetailTransactionAccounting;
  }

  public static Transaction reversedReturnWithSingleItemAndSingleTender() {
    Transaction finRetailTransactionAccounting = buildMockRestaurantScenario("7", "RETURN", true);
    finRetailTransactionAccounting.setTransactionLines(
        Set.of(
            TransactionLineFacade.buildMockRestaurantLine("MI-0", "100.00", "10.00", "0.00"),
            TransactionLineFacade.buildMockRestaurantTender(
                "TId-1", "CREDIT_CARD", "AMERICAN_EXPRESS", null, "110.0", "0.0")));
    return finRetailTransactionAccounting;
  }
}
