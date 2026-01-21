package com.nordstrom.finance.dataintegration.ertm.database.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
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
@Table(name = "retail_transaction_line")
public class RetailTransactionLine {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "retail_transaction_line_seq_generator")
  @SequenceGenerator(
      name = "retail_transaction_line_seq_generator",
      sequenceName = "retail_transaction_line_retail_transaction_line_id_seq",
      allocationSize = 2000)
  @Column(name = "RETAIL_TRANSACTION_LINE_ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TRANSACTION_LINE_ID", nullable = false)
  private TransactionLine transactionLine;

  @Column(name = "DEPARTMENT_ID", length = 100)
  private String departmentId;

  @Column(name = "CLASS_ID", length = 100)
  private String classId;

  @Column(name = "FEE_CODE", length = 100)
  private String feeCode;

  @Column(name = "TENDER_TYPE", length = 100)
  private String tenderType;

  @Column(name = "TENDER_CARD_TYPE_CODE", length = 100)
  private String tenderCardTypeCode;

  @Column(name = "TENDER_CARD_SUBTYPE_CODE", length = 100)
  private String tenderCardSubTypeCode;

  @Column(name = "TENDER_CAPTURE_TYPE", length = 100)
  private String tenderCaptureType;

  @Column(name = "TENDER_ACTIVITY_CODE", length = 100)
  private String tenderActivityCode;

  @Column(name = "LINE_ITEM_AMOUNT")
  private BigDecimal lineItemAmount;

  @Column(name = "TAX_AMOUNT")
  private BigDecimal taxAmount;

  @Column(name = "EMPLOYEE_DISCOUNT_AMOUNT")
  private BigDecimal employeeDiscountAmount;

  @Column(name = "TENDER_AMOUNT")
  private BigDecimal tenderAmount;

  @Column(name = "MID_MERCHANT_ID", length = 100)
  private String midMerchantId;

  @Column(name = "FEE_CODE_GL_STORE_FLAG", length = 100)
  private String feeCodeGlStoreFlag;

  @Column(name = "FEE_CODE_GL_STORE_NUMBER", length = 100)
  private String feeCodeGlStoreNumber;

  @Column(name = "FULFILLMENT_TYPE_DROPSHIP_CODE", length = 100)
  private String fulfillmentTypeDropshipCode;

  @Column(name = "CASH_DISBURSEMENT_LINE1", length = 100)
  private String cashDisbursementLine1;

  @Column(name = "CASH_DISBURSEMENT_LINE2", length = 100)
  private String cashDisbursementLine2;

  @Column(name = "WAIVED_REASON_CODE", length = 100)
  private String waivedReasonCode;

  @Column(name = "WAIVED_AMOUNT")
  private BigDecimal waivedAmount;

  @Column(name = "SUBCLASS_GROUPING", length = 100)
  private String subclassGrouping;
}
