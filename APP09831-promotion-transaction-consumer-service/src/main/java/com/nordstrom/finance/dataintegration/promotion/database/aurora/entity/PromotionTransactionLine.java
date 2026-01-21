package com.nordstrom.finance.dataintegration.promotion.database.aurora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "PROMOTION_TRANSACTION_LINE")
public class PromotionTransactionLine {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "promotion_transaction_line_seq_generator")
  @SequenceGenerator(
      name = "promotion_transaction_line_seq_generator",
      sequenceName = "PROMOTION_TRANSACTION_LINE_PROMOTION_TRANSACTION_LINE_ID_SEQ",
      allocationSize = 2000)
  @Column(name = "PROMOTION_TRANSACTION_LINE_ID")
  private Long promotionTransactionLineId;

  @NonNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TRANSACTION_LINE_ID", nullable = false)
  private TransactionLine transactionLine;

  @Column(name = "PROMO_TYPE", length = 100)
  private String promoType;

  @Column(name = "PROMO_AMOUNT")
  private BigDecimal promoAmount;

  @Column(name = "PROMO_BUSINESS_ORIGIN", length = 100)
  private String promoBusinessOrigin;
}
