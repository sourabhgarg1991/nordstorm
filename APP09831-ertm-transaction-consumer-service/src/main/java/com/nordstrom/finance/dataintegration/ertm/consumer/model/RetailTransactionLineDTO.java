package com.nordstrom.finance.dataintegration.ertm.consumer.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetailTransactionLineDTO {

  @CsvBindByName(column = "SOURCE_REFERENCE_TRANSACTION_ID")
  private String sourceReferenceTransactionId;

  @CsvDate(value = "yyyy-MM-dd")
  @CsvBindByName(column = "SOURCE_PROCESSED_DATE")
  private LocalDate sourceProcessedDate;

  @CsvDate(value = "yyyy-MM-dd")
  @CsvBindByName(column = "TRANSACTION_DATE")
  private LocalDate transactionDate;

  @CsvDate(value = "yyyy-MM-dd")
  @CsvBindByName(column = "BUSINESS_DATE")
  private LocalDate businessDate;

  @CsvBindByName(column = "TRANSACTION_TYPE")
  private String transactionType;

  @CsvBindByName(column = "TRANSACTION_REVERSAL_CODE")
  private String transactionReversalCode;

  @CsvBindByName(column = "SOURCE_REFERENCE_LINE_ID")
  private String sourceReferenceLineId;

  @CsvBindByName(column = "SOURCE_REFERENCE_LINE_TYPE")
  private String sourceReferenceLineType;

  @CsvBindByName(column = "TRANSACTION_LINE_TYPE")
  private String transactionLineType;

  @CsvBindByName(column = "RINGING_STORE")
  private String ringingStore;

  @CsvBindByName(column = "STORE_OF_INTENT")
  private String storeOfIntent;

  @CsvBindByName(column = "DEPARTMENT_ID")
  private String departmentId;

  @CsvBindByName(column = "CLASS_ID")
  private String classId;

  @CsvBindByName(column = "FEE_CODE")
  private String feeCode;

  @CsvBindByName(column = "TENDER_TYPE")
  private String tenderType;

  @CsvBindByName(column = "TENDER_CARD_TYPE")
  private String tenderCardType;

  @CsvBindByName(column = "TENDER_CARD_SUBTYPE")
  private String tenderCardSubType;

  @CsvBindByName(column = "TENDER_ADJUSTMENT_CODE")
  private String tenderAdjustmentCode;

  @CsvBindByName(column = "LINE_ITEM_AMOUNT")
  private BigDecimal lineItemAmount;

  @CsvBindByName(column = "TAX_AMOUNT")
  private BigDecimal taxAmount;

  @CsvBindByName(column = "EMPLOYEE_DISCOUNT_AMOUNT")
  private BigDecimal employeeDiscountAmount;

  @CsvBindByName(column = "TENDER_AMOUNT")
  private BigDecimal tenderAmount;

  @CsvBindByName(column = "CASH_DISBURSEMENT_LINE1")
  private String cashDisbursementLine1;

  @CsvBindByName(column = "CASH_DISBURSEMENT_LINE2")
  private String cashDisbursementLine2;

  @CsvBindByName(column = "WAIVED_REASON_CODE")
  private String waivedReasonCode;

  @CsvBindByName(column = "WAIVED_AMOUNT")
  private BigDecimal waivedAmount;

  @CsvBindByName(column = "FULFILLMENT_TYPE_DROPSHIP_CODE")
  private String fulfillmentTypeDropshipCode;

  @CsvBindByName(column = "DATA_SOURCE_CODE")
  private String dataSourceCode;
}
