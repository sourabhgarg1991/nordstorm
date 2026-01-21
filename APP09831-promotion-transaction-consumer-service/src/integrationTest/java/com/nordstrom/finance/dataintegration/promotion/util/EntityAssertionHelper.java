package com.nordstrom.finance.dataintegration.promotion.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.PromotionTransactionLine;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.Transaction;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.TransactionLine;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;

/**
 * Helper class for entity assertions in integration tests. Provides reusable assertion methods for
 * Transaction, TransactionLine, and PromotionTransactionLine entities.
 */
@UtilityClass
public class EntityAssertionHelper {

  /** Assert Transaction entity fields */
  public static void assertTransaction(
      Transaction actual,
      String expectedGlobalTranId,
      String expectedTransactionType,
      String expectedReversalCode) {

    assertThat(actual).isNotNull();
    assertThat(actual.getTransactionId()).isNotNull();

    // Source reference fields
    assertThat(actual.getSourceReferenceTransactionId())
        .as("Source Reference Transaction ID")
        .isEqualTo(expectedGlobalTranId);
    assertThat(actual.getSourceReferenceSystemType())
        .as("Source Reference System Type")
        .isEqualTo("GCP");

    // Business fields
    assertThat(actual.getSourceReferenceType()).as("Source Reference Type").isEqualTo("PROMO");
    assertThat(actual.getTransactionType())
        .as("Transaction Type")
        .isEqualTo(expectedTransactionType);
    assertThat(actual.getTransactionReversalCode())
        .as("Transaction Reversal Code")
        .isEqualTo(expectedReversalCode);

    // Date fields
    assertThat(actual.getSourceProcessedDate()).as("Source Processed Date").isNotNull();
    assertThat(actual.getBusinessDate()).as("Business Date").isNotNull();
    assertThat(actual.getTransactionDate()).as("Transaction Date").isNotNull();

    // Audit fields
    assertThat(actual.getCreatedDatetime()).as("Created Datetime").isNotNull();
    assertThat(actual.getLastUpdatedDatetime()).as("Last Updated Datetime").isNotNull();

    // Nullable fields
    assertThat(actual.getPartnerRelationshipType()).as("Partner Relationship Type").isNull();
  }

  /** Assert TransactionLine entity fields */
  public static void assertTransactionLine(
      TransactionLine actual, String expectedLineItemId, String expectedLineType) {

    assertThat(actual).isNotNull();
    assertThat(actual.getTransactionLineId()).isNotNull();

    // Source reference fields
    assertThat(actual.getSourceReferenceLineId())
        .as("Source Reference Line ID")
        .isEqualTo(expectedLineItemId);
    assertThat(actual.getSourceReferenceLineType())
        .as("Source Reference Line Type")
        .isEqualTo("PROMO");

    // Business fields
    assertThat(actual.getTransactionLineType())
        .as("Transaction Line Type")
        .isEqualTo(expectedLineType);

    // Store fields
    assertThat(actual.getRingingStore()).as("Ringing Store").isNull();
    assertThat(actual.getStoreOfIntent()).as("Store of Intent").isNotNull();

    // Parent relationship
    assertThat(actual.getTransaction()).as("Parent Transaction").isNotNull();
  }

  /** Assert PromotionTransactionLine entity fields */
  public static void assertPromotionTransactionLine(
      PromotionTransactionLine actual,
      BigDecimal expectedPromoAmount,
      String expectedBusinessOrigin) {

    assertThat(actual).isNotNull();
    assertThat(actual.getPromotionTransactionLineId()).isNotNull();

    // Business fields
    assertThat(actual.getPromoAmount())
        .as("Promo Amount")
        .isEqualByComparingTo(expectedPromoAmount);
    assertThat(actual.getPromoBusinessOrigin())
        .as("Promo Business Origin")
        .isEqualTo(expectedBusinessOrigin);

    // Nullable field
    assertThat(actual.getPromoType()).as("Promo Type").isNull();

    // Parent relationship
    assertThat(actual.getTransactionLine()).as("Parent Transaction Line").isNotNull();
  }

  /** Assert entity IDs are generated (entities were persisted) */
  public static void assertAllEntitiesPersisted(Transaction transaction) {
    assertThat(transaction.getTransactionId()).as("Transaction ID should be generated").isNotNull();

    for (TransactionLine line : transaction.getTransactionLines()) {
      assertThat(line.getTransactionLineId())
          .as("TransactionLine ID should be generated")
          .isNotNull();

      for (PromotionTransactionLine promoLine : line.getPromotionTransactionLines()) {
        assertThat(promoLine.getPromotionTransactionLineId())
            .as("PromotionTransactionLine ID should be generated")
            .isNotNull();
      }
    }
  }
}
