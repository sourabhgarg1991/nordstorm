package com.nordstrom.finance.dataintegration.database.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
@ToString
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

  @Column(name = "PARTNER_RELATIONSHIP_TYPE", length = 50)
  private String partnerRelationshipType;

  @CreationTimestamp
  @Column(name = "CREATED_DATETIME", updatable = false)
  private LocalDateTime createdDateTime;

  @UpdateTimestamp
  @Column(name = "LAST_UPDATED_DATETIME")
  private LocalDateTime lastUpdatedDateTime;

  @OneToMany(
      cascade = {CascadeType.PERSIST, CascadeType.REFRESH},
      mappedBy = "transaction")
  private Set<TransactionLine> transactionLines;

  /**
   * * Helper method to set the transaction lines and maintain the bidirectional relationship
   *
   * @param transactionLines List of TransactionLine to associate with this Transaction
   */
  public void setTransactionLines(Set<TransactionLine> transactionLines) {
    if (this.transactionLines == null) this.transactionLines = new HashSet<>();
    for (TransactionLine transactionLine : transactionLines) {
      // This is necessary to maintain the bidirectional relationship
      transactionLine.setTransaction(this);
    }
    this.transactionLines = transactionLines;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Transaction that = (Transaction) o;
    return Objects.equals(transactionId, that.transactionId)
        && Objects.equals(sourceReferenceTransactionId, that.sourceReferenceTransactionId)
        && Objects.equals(sourceReferenceSystemType, that.sourceReferenceSystemType)
        && Objects.equals(sourceReferenceType, that.sourceReferenceType)
        && Objects.equals(sourceProcessedDate, that.sourceProcessedDate)
        && Objects.equals(transactionDate, that.transactionDate)
        && Objects.equals(businessDate, that.businessDate)
        && Objects.equals(transactionType, that.transactionType)
        && Objects.equals(transactionReversalCode, that.transactionReversalCode)
        && Objects.equals(partnerRelationshipType, that.partnerRelationshipType)
        && transactionLines.containsAll(that.transactionLines);
  }
}
