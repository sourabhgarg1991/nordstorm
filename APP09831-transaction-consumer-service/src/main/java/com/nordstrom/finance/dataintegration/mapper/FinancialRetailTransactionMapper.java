package com.nordstrom.finance.dataintegration.mapper;

import static com.nordstrom.finance.dataintegration.common.util.StringFormatUtility.toFourDigitFormat;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.*;
import static com.nordstrom.finance.dataintegration.utility.DateUtility.toLocalDate;
import static com.nordstrom.finance.dataintegration.utility.MoneyUtility.getAmount;

import com.nordstrom.customer.object.operational.*;
import com.nordstrom.finance.dataintegration.database.entity.MarketplaceTransactionLine;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.exception.*;
import com.nordstrom.finance.dataintegration.fortknox.FortKnoxRedemptionService;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortknoxException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialRetailTransactionMapper {

  private final FortKnoxRedemptionService fortKnoxRedemptionService;

  @Value("${app.fortknox.redeem}")
  boolean fortknoxRedeemEnabled;

  /**
   * Maps a FinancialRetailTransaction to a Transaction entity.
   *
   * @param marketplaceTransaction the FinancialRetailTransaction to map
   * @return the mapped Transaction entity
   * @throws ObjectMappingException if mapping fails
   */
  public Transaction toTransaction(FinancialRetailTransaction marketplaceTransaction)
      throws ObjectMappingException, FortknoxException {
    if (marketplaceTransaction == null) {
      log.warn("FinancialRetailTransaction is null, returning null Transaction");
      return null;
    }
    try {
      log.debug(
          "Mapping FinancialRetailTransaction [{}] to Transaction",
          marketplaceTransaction.getFinancialRetailTransactionRecordId());

      Transaction transaction =
          Transaction.builder()
              .sourceReferenceTransactionId(
                  String.valueOf(marketplaceTransaction.getFinancialRetailTransactionRecordId()))
              .sourceProcessedDate(toLocalDate(marketplaceTransaction.getCreationTime()))
              .businessDate(marketplaceTransaction.getBusinessDate())
              .transactionType(marketplaceTransaction.getTransactionType().name())
              .transactionReversalCode(marketplaceTransaction.getIsReversed() ? "Y" : "N")
              .sourceReferenceSystemType(SOURCE_REFERENCE_SYSTEM_TYPE_SDM)
              .sourceReferenceType(SOURCE_REFERENCE_TYPE_MARKETPLACE)
              .transactionDate(toLocalDate(marketplaceTransaction.getTransactionTime()))
              .partnerRelationshipType(partnerRelationshipTypeName(marketplaceTransaction))
              .build();
      transaction.setTransactionLines(mapTransactionLines(marketplaceTransaction));
      log.debug("Successfully mapped FinancialRetailTransaction to Transaction");
      return transaction;
    } catch (FortknoxException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error mapping FinancialRetailTransaction to Transaction", e);
      throw new ObjectMappingException(
          "Failed to map FinancialRetailTransaction to Transaction", e);
    }
  }

  /**
   * Maps line items and tenders to TransactionLine entities, assigning the provided parent. Caller
   * still attaches the returned list to parent (for cascade).
   */
  private Set<TransactionLine> mapTransactionLines(
      FinancialRetailTransaction marketplaceTransaction)
      throws ObjectMappingException, FortknoxException {

    try {
      log.debug("Started mapping TransactionLines");
      Set<TransactionLine> transactionLines = new HashSet<>();

      // Merchandise
      List<FinancialRetailTransactionMerchandiseLineItem> merchandiseItems =
          marketplaceTransaction.getMerchandiseLineItems();
      if (merchandiseItems != null) {
        for (FinancialRetailTransactionMerchandiseLineItem item : merchandiseItems) {
          if (item == null) continue;
          transactionLines.add(buildItemTransactionLine(marketplaceTransaction, item));
        }
      }

      // Non-Merchandise
      List<FinancialRetailTransactionNonMerchandiseLineItem> nonMerchandiseItems =
          marketplaceTransaction.getNonMerchandiseLineItems();
      if (nonMerchandiseItems != null) {
        for (FinancialRetailTransactionNonMerchandiseLineItem item : nonMerchandiseItems) {
          if (item == null) continue;
          transactionLines.add(buildItemTransactionLine(marketplaceTransaction, item));
        }
      }

      // Tenders
      List<FinancialRetailTransactionTenderDetail> tenderDetails =
          marketplaceTransaction.getTenderDetails();
      if (tenderDetails != null) {
        for (FinancialRetailTransactionTenderDetail tenderDetail : tenderDetails) {
          if (tenderDetail == null) continue;
          transactionLines.add(buildTenderTransactionLine(marketplaceTransaction, tenderDetail));
        }
      }
      log.info("Total mapped {} TransactionLine(s)", transactionLines.size());
      return transactionLines;
    } catch (FortknoxException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error mapping FinancialRetailTransaction to TransactionLines", e);
      throw new ObjectMappingException(
          "Failed to map FinancialRetailTransaction to TransactionLines", e);
    }
  }

  private String partnerRelationshipTypeName(FinancialRetailTransaction transaction) {
    return transaction.getPartnerRelationship().isPresent()
        ? transaction.getPartnerRelationship().map(p -> p.getType().name()).orElse(null)
        : null;
  }

  private TransactionLine buildItemTransactionLine(
      FinancialRetailTransaction marketplaceTransaction,
      FinancialRetailTransactionMerchandiseLineItem lineItem)
      throws FortknoxException {
    TransactionLine transactionLine =
        TransactionLine.builder()
            .sourceReferenceLineId(String.valueOf(lineItem.getTransactionLineId()))
            .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_ITEM)
            .storeOfIntent(String.valueOf(lineItem.getIntentLocationId()))
            .ringingStore(String.valueOf(marketplaceTransaction.getRetailLocationId()))
            .build();
    transactionLine.setMarketplaceTransactionLine(buildMarketplaceLineForMerch(lineItem));
    return transactionLine;
  }

  private TransactionLine buildItemTransactionLine(
      FinancialRetailTransaction marketplaceTransaction,
      FinancialRetailTransactionNonMerchandiseLineItem lineItem) {
    TransactionLine transactionLine =
        TransactionLine.builder()
            .sourceReferenceLineId(String.valueOf(lineItem.getTransactionLineId()))
            .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_ITEM)
            .storeOfIntent(String.valueOf(lineItem.getIntentLocationId()))
            .ringingStore(String.valueOf(marketplaceTransaction.getRetailLocationId()))
            .build();
    transactionLine.setMarketplaceTransactionLine(buildMarketplaceLineForNonMerch(lineItem));
    return transactionLine;
  }

  private TransactionLine buildTenderTransactionLine(
      FinancialRetailTransaction marketplaceTransaction,
      FinancialRetailTransactionTenderDetail tenderDetail) {
    TransactionLine transactionLine =
        TransactionLine.builder()
            .sourceReferenceLineId(tenderDetail.getTenderType().name())
            .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_TENDER)
            .ringingStore(String.valueOf(marketplaceTransaction.getRetailLocationId()))
            .build();
    transactionLine.setMarketplaceTransactionLine(buildMarketplaceLineForTender(tenderDetail));
    return transactionLine;
  }

  private MarketplaceTransactionLine buildMarketplaceLineForMerch(
      FinancialRetailTransactionMerchandiseLineItem item) throws FortknoxException {
    return MarketplaceTransactionLine.builder()
        .lineItemAmount(getAmount(item.getSalePrice()))
        .taxAmount(item.getTax() != null ? getAmount(item.getTax().getTaxTotal()) : null)
        .refundAdjustmentReasonCode(
            item.getReturnDetail()
                .flatMap(FinancialRetailTransactionMerchandiseItemReturnDetail::getRefundAdjustment)
                .map(FinancialRetailTransactionItemRefundAdjustment::getAdjustmentReason)
                .map(Enum::name)
                .orElse(null))
        .marketplaceJwnCommissionAmount(getJwnCommissionAmount(item))
        .build();
  }

  private BigDecimal getJwnCommissionAmount(FinancialRetailTransactionMerchandiseLineItem item)
      throws FortknoxException {
    if (fortknoxRedeemEnabled) {
      return item.getRevenueRecognizedDetail().isPresent()
          ? fortKnoxRedemptionService.redeemAndGetFullAmount(
              item.getRevenueRecognizedDetail().get().getRevenueRecognized())
          : null;
    }
    BigDecimal commissionPercentage = new BigDecimal("0.10");
    return getAmount(item.getSalePrice())
        .multiply(commissionPercentage)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private MarketplaceTransactionLine buildMarketplaceLineForNonMerch(
      FinancialRetailTransactionNonMerchandiseLineItem item) {
    return MarketplaceTransactionLine.builder()
        .lineItemAmount(getAmount(item.getSalePrice()))
        .taxAmount(item.getTax() != null ? getAmount(item.getTax().getTaxTotal()) : null)
        .feeCode(
            item.getFeeCode() != null ? toFourDigitFormat(String.valueOf(item.getFeeCode())) : null)
        .build();
  }

  private MarketplaceTransactionLine buildMarketplaceLineForTender(
      FinancialRetailTransactionTenderDetail tenderDetail) {
    return MarketplaceTransactionLine.builder()
        .tenderAmount(getAmount(tenderDetail.getTotal()))
        .tenderType(tenderDetail.getTenderType().name())
        .build();
  }
}
