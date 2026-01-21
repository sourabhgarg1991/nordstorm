package com.nordstrom.finance.dataintegration.database.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode
@Table(name = "RESTAURANT_TRANSACTION_LINE")
public class RestaurantTransactionLine {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "restaurant_transaction_line_seq_generator")
  @SequenceGenerator(
      name = "restaurant_transaction_line_seq_generator",
      sequenceName = "RESTAURANT_TRANSACTION_LINE_RESTAURANT_TRANSACTION_LINE_ID_SEQ",
      allocationSize = 2000)
  @Column(name = "RESTAURANT_TRANSACTION_LINE_ID")
  private Long restaurantTransactionLineId;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TRANSACTION_LINE_ID")
  private TransactionLine transactionLine;

  @EqualsAndHashCode.Exclude
  @Column(name = "LINE_ITEM_AMOUNT")
  private BigDecimal lineItemAmount;

  @EqualsAndHashCode.Exclude
  @Column(name = "EMPLOYEE_DISCOUNT_AMOUNT")
  private BigDecimal employeeDiscountAmount;

  @EqualsAndHashCode.Exclude
  @Column(name = "TAX_AMOUNT")
  private BigDecimal taxAmount;

  @EqualsAndHashCode.Exclude
  @Column(name = "TENDER_AMOUNT")
  private BigDecimal tenderAmount;

  @EqualsAndHashCode.Exclude
  @Column(name = "RESTAURANT_TIP_AMOUNT")
  private BigDecimal restaurantTipAmount;

  @Column(name = "DEPARTMENT_ID", length = 100)
  private String departmentId;

  @Column(name = "CLASS_ID", length = 100)
  private String classId;

  @Column(name = "TENDER_TYPE", length = 100)
  private String tenderType;

  @Column(name = "TENDER_CARD_TYPE_CODE", length = 100)
  private String tenderCardTypeCode;

  @Column(name = "TENDER_CARD_SUBTYPE_CODE", length = 100)
  private String tenderCardSubTypeCode;

  @Column(name = "TENDER_CAPTURE_TYPE", length = 100)
  private String tenderCaptureType;

  @Column(name = "RESTAURANT_LOYALTY_BENEFIT_TYPE", length = 100)
  private String restaurantLoyaltyBenefitType;

  @Column(name = "RESTAURANT_DELIVERY_PARTNER", length = 100)
  private String restaurantDeliveryPartner;

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsLineItemAmount() {
    return lineItemAmount != null ? lineItemAmount.stripTrailingZeros() : null;
  }

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsTaxAmount() {
    return taxAmount != null ? taxAmount.stripTrailingZeros() : null;
  }

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsEmployeeDiscountAmount() {
    return employeeDiscountAmount != null ? employeeDiscountAmount.stripTrailingZeros() : null;
  }

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsTenderAmount() {
    return tenderAmount != null ? tenderAmount.stripTrailingZeros() : null;
  }

  @EqualsAndHashCode.Include
  private BigDecimal getEqualsRestaurantTipAmount() {
    return restaurantTipAmount != null ? restaurantTipAmount.stripTrailingZeros() : null;
  }
}
