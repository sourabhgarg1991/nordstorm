package com.nordstrom.finance.dataintegration.database.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode
@Table(name = "MARKETPLACE_TRANSACTION_LINE")
public class MarketplaceTransactionLine {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "marketplace_transaction_line_seq_generator")
  @SequenceGenerator(
      name = "marketplace_transaction_line_seq_generator",
      sequenceName = "MARKETPLACE_TRANSACTION_LINE_MARKETPLACE_TRANSACTION_LINE_I_SEQ",
      allocationSize = 2000)
  @Column(name = "MARKETPLACE_TRANSACTION_LINE_ID")
  private Long marketplaceTransactionLineId;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TRANSACTION_LINE_ID")
  private TransactionLine transactionLine;

  @Column(name = "PARTNER_RELATIONSHIP_TYPE")
  private String partnerRelationshipType;

  @EqualsAndHashCode.Exclude
  @Column(name = "LINE_ITEM_AMOUNT")
  private BigDecimal lineItemAmount;

  @EqualsAndHashCode.Exclude
  @Column(name = "TAX_AMOUNT")
  private BigDecimal taxAmount;

  @EqualsAndHashCode.Exclude
  @Column(name = "TENDER_AMOUNT")
  private BigDecimal tenderAmount;

  @EqualsAndHashCode.Exclude
  @Column(name = "MARKETPLACE_JWN_COMMISSION_AMOUNT")
  private BigDecimal marketplaceJwnCommissionAmount;

  @Column(name = "FEE_CODE")
  private String feeCode;

  @Column(name = "TENDER_TYPE")
  private String tenderType;

  @Column(name = "REFUND_ADJUSTMENT_REASON_CODE")
  private String refundAdjustmentReasonCode;

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsLineItemAmount() {
    return lineItemAmount != null ? lineItemAmount.stripTrailingZeros() : null;
  }

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsTaxAmount() {
    return taxAmount != null ? taxAmount.stripTrailingZeros() : null;
  }

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsMarketplaceJwnCommissionAmount() {
    return marketplaceJwnCommissionAmount != null
        ? marketplaceJwnCommissionAmount.stripTrailingZeros()
        : null;
  }

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsTenderAmount() {
    return tenderAmount != null ? tenderAmount.stripTrailingZeros() : null;
  }
}
