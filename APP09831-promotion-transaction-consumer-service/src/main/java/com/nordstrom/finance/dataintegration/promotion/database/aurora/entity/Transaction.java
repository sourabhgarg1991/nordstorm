package com.nordstrom.finance.dataintegration.promotion.database.aurora.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "TRANSACTION")
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_seq_generator")
  @SequenceGenerator(
      name = "transaction_seq_generator",
      sequenceName = "TRANSACTION_TRANSACTION_ID_SEQ",
      allocationSize = 2000)
  @Column(name = "TRANSACTION_ID")
  private Long transactionId;

  @NonNull
  @Column(name = "SOURCE_REFERENCE_TRANSACTION_ID")
  private String sourceReferenceTransactionId;

  @NonNull
  @Column(name = "SOURCE_REFERENCE_SYSTEM_TYPE")
  private String sourceReferenceSystemType;

  @NonNull
  @Column(name = "SOURCE_REFERENCE_TYPE")
  private String sourceReferenceType;

  @NonNull
  @Column(name = "SOURCE_PROCESSED_DATE")
  private LocalDate sourceProcessedDate;

  @NonNull
  @Column(name = "TRANSACTION_DATE")
  private LocalDate transactionDate;

  @NonNull
  @Column(name = "BUSINESS_DATE")
  private LocalDate businessDate;

  @NonNull
  @Column(name = "TRANSACTION_TYPE")
  private String transactionType;

  @NonNull
  @Column(
      name = "TRANSACTION_REVERSAL_CODE",
      columnDefinition = "char(1)",
      length = 1,
      nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String transactionReversalCode;

  @Column(name = "PARTNER_RELATIONSHIP_TYPE", length = 100)
  private String partnerRelationshipType;

  @Column(name = "CREATED_DATETIME", updatable = false)
  private LocalDateTime createdDatetime;

  @Column(name = "LAST_UPDATED_DATETIME")
  private LocalDateTime lastUpdatedDatetime;

  // Bidirectional relationship with cascade
  @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TransactionLine> transactionLines = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdDatetime = LocalDateTime.now();
    lastUpdatedDatetime = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    lastUpdatedDatetime = LocalDateTime.now();
  }
}
