package com.nordstrom.finance.dataintegration.ertm.database.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaction")
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_seq_generator")
  @SequenceGenerator(
      name = "transaction_seq_generator",
      sequenceName = "transaction_transaction_id_seq",
      allocationSize = 2000)
  @Column(name = "TRANSACTION_ID")
  private Long id;

  @Column(name = "SOURCE_REFERENCE_TRANSACTION_ID", nullable = false, length = 100)
  private String sourceReferenceTransactionId;

  @Column(name = "SOURCE_REFERENCE_SYSTEM_TYPE", nullable = false, length = 50)
  private String sourceReferenceSystemType;

  @Column(name = "SOURCE_REFERENCE_TYPE", nullable = false, length = 50)
  private String sourceReferenceType;

  @Column(name = "SOURCE_PROCESSED_DATE", nullable = false)
  private LocalDate sourceProcessedDate;

  @Column(name = "TRANSACTION_DATE", nullable = false)
  private LocalDate transactionDate;

  @Column(name = "BUSINESS_DATE", nullable = false)
  private LocalDate businessDate;

  @Column(name = "TRANSACTION_TYPE", nullable = false, length = 50)
  private String transactionType;

  @NonNull
  @Column(
      name = "TRANSACTION_REVERSAL_CODE",
      columnDefinition = "char(1)",
      length = 1,
      nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String transactionReversalCode;

  @CreationTimestamp
  @Column(name = "CREATED_DATETIME", updatable = false)
  private LocalDateTime createdDateTime;

  @UpdateTimestamp
  @Column(name = "LAST_UPDATED_DATETIME")
  private LocalDateTime lastUpdatedDateTime;

  @OneToMany(
      mappedBy = "transaction",
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @Singular
  private List<TransactionLine> transactionLines;
}
