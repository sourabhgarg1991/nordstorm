package com.nordstrom.finance.dataintegration.promotion.database.aurora.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "TRANSACTION_LINE")
public class TransactionLine {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_line_seq_generator")
  @SequenceGenerator(
      name = "transaction_line_seq_generator",
      sequenceName = "TRANSACTION_LINE_TRANSACTION_LINE_ID_SEQ",
      allocationSize = 2000)
  @Column(name = "TRANSACTION_LINE_ID")
  private Long transactionLineId;

  @NonNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TRANSACTION_ID", nullable = false)
  private Transaction transaction;

  @NonNull
  @Column(name = "SOURCE_REFERENCE_LINE_ID", length = 100)
  private String sourceReferenceLineId;

  @NonNull
  @Column(name = "SOURCE_REFERENCE_LINE_TYPE", length = 100)
  private String sourceReferenceLineType;

  @Column(name = "TRANSACTION_LINE_TYPE", length = 50)
  private String transactionLineType;

  @Column(name = "RINGING_STORE", length = 10)
  private String ringingStore;

  @Column(name = "STORE_OF_INTENT", length = 10)
  private String storeOfIntent;

  // Bidirectional relationship with cascade
  @OneToMany(mappedBy = "transactionLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<PromotionTransactionLine> promotionTransactionLines = new ArrayList<>();
}
