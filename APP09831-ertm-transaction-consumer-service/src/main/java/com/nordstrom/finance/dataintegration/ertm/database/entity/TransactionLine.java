package com.nordstrom.finance.dataintegration.ertm.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaction_line")
public class TransactionLine {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_line_seq_generator")
  @SequenceGenerator(
      name = "transaction_line_seq_generator",
      sequenceName = "transaction_line_transaction_line_id_seq",
      allocationSize = 2000)
  @Column(name = "TRANSACTION_LINE_ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TRANSACTION_ID", nullable = false)
  private Transaction transaction;

  @Column(name = "SOURCE_REFERENCE_LINE_ID", nullable = false, length = 100)
  private String sourceReferenceLineId;

  @Column(name = "SOURCE_REFERENCE_LINE_TYPE", nullable = false, length = 100)
  private String sourceReferenceLineType;

  @Column(name = "TRANSACTION_LINE_TYPE", length = 50)
  private String transactionLineType;

  @Column(name = "RINGING_STORE", length = 10)
  private String ringingStore;

  @Column(name = "STORE_OF_INTENT", length = 10)
  private String storeOfIntent;

  @OneToOne(mappedBy = "transactionLine", cascade = CascadeType.ALL, orphanRemoval = true)
  private RetailTransactionLine retailTransactionLine;
}
