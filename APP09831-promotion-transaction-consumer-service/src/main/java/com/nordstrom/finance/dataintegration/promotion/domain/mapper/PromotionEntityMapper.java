package com.nordstrom.finance.dataintegration.promotion.domain.mapper;

import com.nordstrom.finance.dataintegration.common.util.StringFormatUtility;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.PromotionTransactionLine;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.Transaction;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.promotion.domain.model.LineItemDetailVO;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapper class for converting TransactionDetailVO and related VOs to JPA entities. Provides modular
 * mapping methods to support duplicate checking and flexible processing.
 */
@Slf4j
@UtilityClass
public class PromotionEntityMapper {

  private static final String SOURCE_SYSTEM_TYPE = "GCP";
  private static final String SOURCE_REFERENCE_TYPE = "PROMO";
  private static final String SOURCE_REFERENCE_LINE_TYPE = "PROMO";
  private static final String REVERSAL_YES = "Y";
  private static final String REVERSAL_NO = "N";

  /**
   * Maps a list of TransactionDetailVOs to Transaction entities with all children.
   *
   * @param transactionDetailVOs list of source VOs
   * @return list of fully populated Transaction entities
   */
  public static List<Transaction> mapToTransactions(
      List<TransactionDetailVO> transactionDetailVOs) {
    if (transactionDetailVOs == null || transactionDetailVOs.isEmpty()) {
      return new ArrayList<>();
    }

    return transactionDetailVOs.stream()
        .filter(Objects::nonNull)
        .map(PromotionEntityMapper::mapToTransactionWithChildren)
        .toList();
  }

  /**
   * Maps a TransactionDetailVO to a complete Transaction entity with all related children. This
   * method creates the full entity graph including TransactionLines and PromotionTransactionLines.
   *
   * @param transactionDetailVO the source VO containing transaction details
   * @return a fully populated Transaction entity with cascade-ready relationships
   */
  public static Transaction mapToTransactionWithChildren(TransactionDetailVO transactionDetailVO) {
    Transaction transaction = mapToTransaction(transactionDetailVO);

    if (transactionDetailVO.lineItems() != null && !transactionDetailVO.lineItems().isEmpty()) {
      for (LineItemDetailVO lineItem : transactionDetailVO.lineItems()) {
        TransactionLine transactionLine = mapToTransactionLine(lineItem, transaction);

        // Note: Currently this is 1:1 relationship between TransactionLine and
        // PromotionTransactionLine
        // but the entity structure supports 1:many for future extensibility
        PromotionTransactionLine promotionLine =
            mapToPromotionTransactionLine(lineItem, transactionLine);

        transaction.getTransactionLines().add(transactionLine);
        transactionLine.getPromotionTransactionLines().add(promotionLine);
      }
    }

    log.debug(
        "Mapped TransactionDetailVO with globalTransactionId={} to Transaction entity with {} lines",
        transactionDetailVO.globalTransactionId(),
        transaction.getTransactionLines().size());

    return transaction;
  }

  /**
   * Maps a TransactionDetailVO to a Transaction entity without children. Useful for duplicate
   * checking before full entity creation.
   *
   * @param transactionDetailVO the source VO
   * @return a Transaction entity without child relationships
   */
  public static Transaction mapToTransaction(TransactionDetailVO transactionDetailVO) {
    // Get the first line item to extract transaction-level data
    LineItemDetailVO firstLineItem = transactionDetailVO.lineItems().getFirst();

    return Transaction.builder()
        .sourceReferenceTransactionId(transactionDetailVO.globalTransactionId())
        .sourceReferenceSystemType(SOURCE_SYSTEM_TYPE)
        .sourceReferenceType(SOURCE_REFERENCE_TYPE)
        .sourceProcessedDate(firstLineItem.firstReportedTmstp())
        .transactionDate(
            LocalDate.now()) // reserved for ERTM&Restaurant - use current date as placeholder
        .businessDate(transactionDetailVO.businessDate())
        .transactionType(firstLineItem.transactionCode().getCode())
        .transactionReversalCode(firstLineItem.isReversed() ? REVERSAL_YES : REVERSAL_NO)
        .partnerRelationshipType(null)
        .transactionLines(new ArrayList<>())
        .build();
  }

  /**
   * Maps a LineItemDetailVO to a TransactionLine entity without relationships.
   *
   * @param lineItemDetailVO the source VO
   * @return a TransactionLine entity
   */
  public static TransactionLine mapToTransactionLine(
      LineItemDetailVO lineItemDetailVO, Transaction transaction) {

    return TransactionLine.builder()
        .transaction(transaction)
        .sourceReferenceLineId(lineItemDetailVO.lineItemId())
        .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE)
        .transactionLineType(lineItemDetailVO.transactionActivityCode().getActivityCode())
        .ringingStore(null)
        .storeOfIntent(StringFormatUtility.toFourDigitFormat(lineItemDetailVO.store()))
        .promotionTransactionLines(new ArrayList<>()) // Initialize empty list
        .build();
  }

  /**
   * Maps a LineItemDetailVO to a PromotionTransactionLine entity without relationships.
   *
   * @param lineItemDetailVO the source VO
   * @return a PromotionTransactionLine entity
   */
  public static PromotionTransactionLine mapToPromotionTransactionLine(
      LineItemDetailVO lineItemDetailVO, TransactionLine transactionLine) {

    return PromotionTransactionLine.builder()
        .transactionLine(transactionLine)
        .promoType(null)
        .promoAmount(lineItemDetailVO.discountAmount())
        .promoBusinessOrigin(lineItemDetailVO.businessOrigin().name())
        .build();
  }
}
