package com.nordstrom.finance.dataintegration.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@ToString
@Table(name = "FILTERED_TRANSACTION")
@EqualsAndHashCode
public class FilteredTransaction {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "filtered_transaction_seq_generator")
  @SequenceGenerator(
      name = "filtered_transaction_seq_generator",
      sequenceName = "FILTERED_TRANSACTION_FILTERED_TRANSACTION_ID_SEQ",
      allocationSize = 1)
  @Column(name = "FILTERED_TRANSACTION_ID")
  private Long filterTransactionId;

  @Column(name = "SOURCE_REFERENCE_TRANSACTION_ID")
  private String sourceReferenceTransactionId;

  @Column(name = "SOURCE_REFERENCE_SYSTEM_TYPE")
  private String sourceReferenceSystemType;

  @Column(name = "SOURCE_REFERENCE_TYPE")
  private String sourceReferenceType;

  @Column(name = "FILTERED_REASON")
  private String filteredReason;

  @CreationTimestamp
  @Column(name = "CREATED_DATETIME", updatable = false)
  private LocalDateTime createdDateTime;

  @UpdateTimestamp
  @Column(name = "LAST_UPDATED_DATETIME")
  private LocalDateTime lastUpdatedDateTime;
}
