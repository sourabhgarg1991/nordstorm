package com.nordstrom.finance.dataintegration.database.entity;

import jakarta.persistence.*;
import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "TRANSACTION_LINE")
@EqualsAndHashCode
public class TransactionLine {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_line_seq_generator")
  @SequenceGenerator(
      name = "transaction_line_seq_generator",
      sequenceName = "TRANSACTION_LINE_TRANSACTION_LINE_ID_SEQ",
      allocationSize = 2000)
  @Column(name = "TRANSACTION_LINE_ID")
  private Long transactionLineId;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false)
  @JoinColumn(name = "TRANSACTION_ID", nullable = false)
  private Transaction transaction;

  @Column(name = "SOURCE_REFERENCE_LINE_ID")
  private String sourceReferenceLineId;

  @Column(name = "SOURCE_REFERENCE_LINE_TYPE")
  private String sourceReferenceLineType;

  @Column(name = "TRANSACTION_LINE_TYPE")
  private String transactionLineType;

  @Column(name = "RINGING_STORE")
  private String ringingStore;

  @Column(name = "STORE_OF_INTENT")
  private String storeOfIntent;

  @OneToOne(
      mappedBy = "transactionLine",
      cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
  private RestaurantTransactionLine restaurantTransactionLine;

  @OneToOne(
      mappedBy = "transactionLine",
      cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
  private MarketplaceTransactionLine marketplaceTransactionLine;

  /**
   * * Sets the restaurant transaction line for this transaction line.
   *
   * @param restaurantTransactionLine the restaurant transaction line to be associated with this
   *     transaction line
   */
  public void setRestaurantTransactionLine(RestaurantTransactionLine restaurantTransactionLine) {
    if (restaurantTransactionLine == null) {
      if (this.restaurantTransactionLine != null) {
        this.restaurantTransactionLine.setTransactionLine(null);
      }
    } else {
      restaurantTransactionLine.setTransactionLine(this);
    }
    this.restaurantTransactionLine = restaurantTransactionLine;
  }

  /**
   * Sets the marketplace transaction line for this transaction line.
   *
   * @param marketplaceTransactionLine the marketplace transaction line to be associated with this
   *     transaction line
   */
  public void setMarketplaceTransactionLine(MarketplaceTransactionLine marketplaceTransactionLine) {
    if (marketplaceTransactionLine == null) {
      if (this.marketplaceTransactionLine != null) {
        this.marketplaceTransactionLine.setTransactionLine(null);
      }
    } else {
      marketplaceTransactionLine.setTransactionLine(this);
    }
    this.marketplaceTransactionLine = marketplaceTransactionLine;
  }
}
