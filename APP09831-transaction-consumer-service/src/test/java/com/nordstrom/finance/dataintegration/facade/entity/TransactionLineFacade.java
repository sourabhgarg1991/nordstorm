package com.nordstrom.finance.dataintegration.facade.entity;

import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_LINE_TYPE_ITEM;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_LINE_TYPE_TENDER;

import com.nordstrom.finance.dataintegration.database.entity.MarketplaceTransactionLine;
import com.nordstrom.finance.dataintegration.database.entity.RestaurantTransactionLine;
import com.nordstrom.finance.dataintegration.database.entity.TransactionLine;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;

public class TransactionLineFacade {
  private TransactionLineFacade() {}

  private static TransactionLine.TransactionLineBuilder getDefaultBuilder() {
    return TransactionLine.builder();
  }

  public static TransactionLine build(
      UnaryOperator<TransactionLine.TransactionLineBuilder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static TransactionLine buildMockMarketPlaceLine(
      String sourceReferenceLineId,
      String feeCode,
      String lineItemAmount,
      String taxAmount,
      String commissionAmount) {
    TransactionLine transactionLine =
        build(
            builder ->
                builder
                    .sourceReferenceLineId(sourceReferenceLineId)
                    .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_ITEM)
                    .storeOfIntent("ILId-1")
                    .ringingStore("RLId-1"));
    transactionLine.setMarketplaceTransactionLine(
        MarketplaceTransactionLine.builder()
            .lineItemAmount(new BigDecimal(lineItemAmount))
            .taxAmount(new BigDecimal(taxAmount))
            .marketplaceJwnCommissionAmount(
                commissionAmount == null ? null : new BigDecimal(commissionAmount))
            .feeCode(feeCode)
            .build());
    return transactionLine;
  }

  public static TransactionLine buildMockMarketPlaceTender(
      String sourceReferenceLineId, String tenderType, String tenderAmount) {
    TransactionLine transactionLine =
        build(
            builder ->
                builder
                    .sourceReferenceLineId(sourceReferenceLineId)
                    .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_TENDER)
                    .storeOfIntent(null)
                    .ringingStore("RLId-1"));
    transactionLine.setMarketplaceTransactionLine(
        MarketplaceTransactionLine.builder()
            .tenderType(tenderType)
            .tenderAmount(new BigDecimal(tenderAmount))
            .build());
    return transactionLine;
  }

  public static TransactionLine buildMockRestaurantTender(
      String sourceReferenceLineId,
      String tenderType,
      String cardType,
      String subType,
      String tenderAmount,
      String tipAmount) {
    TransactionLine transactionLine =
        build(
            builder ->
                builder
                    .sourceReferenceLineId(sourceReferenceLineId)
                    .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_TENDER)
                    .ringingStore("0100"));
    transactionLine.setRestaurantTransactionLine(
        RestaurantTransactionLine.builder()
            .tenderAmount(new BigDecimal(tenderAmount))
            .restaurantTipAmount(new BigDecimal(tipAmount))
            .tenderType(tenderType)
            .tenderCardTypeCode(cardType)
            .tenderCardSubTypeCode(subType)
            .build());
    return transactionLine;
  }

  public static TransactionLine buildMockRestaurantLine(
      String sourceReferenceLineId,
      String lineItemAmount,
      String taxAmount,
      String employeeDiscountAmount) {
    TransactionLine transactionLine =
        build(
            builder ->
                builder
                    .sourceReferenceLineId(sourceReferenceLineId)
                    .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_ITEM)
                    .ringingStore("0100"));
    transactionLine.setRestaurantTransactionLine(
        RestaurantTransactionLine.builder()
            .lineItemAmount(new BigDecimal(lineItemAmount))
            .taxAmount(new BigDecimal(taxAmount))
            .employeeDiscountAmount(new BigDecimal(employeeDiscountAmount))
            .departmentId("0005")
            .classId("0001")
            .build());
    return transactionLine;
  }
}
