package com.nordstrom.finance.dataintegration.transactionaggregator.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Builder
@ToString
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Transaction_Aggregation_Relation")
public class TransactionAggregationRelationEntity {

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "transaction_aggregation_relation_seq_generator")
  @SequenceGenerator(
      name = "transaction_aggregation_relation_seq_generator",
      sequenceName = "transaction_aggregation_relat_transaction_aggregation_relat_seq",
      allocationSize = 2000)
  @Column(name = "TRANSACTION_AGGREGATION_RELATION_ID")
  private Long transactionAggregationRelationId;

  @Column(name = "AGGREGATION_ID")
  private UUID aggregationId;

  @Column(name = "TRANSACTION_LINE_ID")
  private Long transactionLineId;

  @Column(name = "IS_PUBLISHED_TO_DATA_PLATFORM")
  private Boolean isPublishedToDataPlatform;

  @EqualsAndHashCode.Exclude
  @CreationTimestamp
  @Column(name = "CREATED_DATETIME")
  private LocalDateTime createdDatetime;

  @EqualsAndHashCode.Exclude
  @Column(name = "LAST_UPDATED_DATETIME")
  @UpdateTimestamp
  private LocalDateTime lastUpdatedDatetime;
}
