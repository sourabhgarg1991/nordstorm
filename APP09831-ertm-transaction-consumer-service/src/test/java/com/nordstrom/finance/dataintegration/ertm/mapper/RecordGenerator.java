package com.nordstrom.finance.dataintegration.ertm.mapper;

import com.nordstrom.finance.dataintegration.common.util.StringFormatUtility;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.RetailTransactionLine;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.entity.TransactionLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RecordGenerator {

  public static String[] getDefaultRetailTransaction() {
    ArrayList<String> list = new ArrayList();

    list.add("1");
    list.add("2023-04-26");
    list.add("2023-06-15");
    list.add("2022-12-12");
    list.add("808");
    list.add("SALE");
    list.add("N");
    list.add("ItemLine");
    list.add("2");
    list.add("S");
    list.add("808");
    list.add("802");
    list.add("63");
    list.add("151");
    list.add("Cash1");
    list.add("Cash2");
    list.add("StoreShipSend");
    list.add("Waived");
    list.add("0.0");
    list.add("2.0");
    list.add("3.0");
    list.add("4.0");
    list.add("NC");
    list.add("0");
    list.add("RR");
    list.add("Adj");
    list.add("64.95");

    return list.toArray(new String[0]);
  }

  public static RetailTransactionLineDTO generateRetailTransactionLineDto() {
    RetailTransactionLineDTO retailTransactionLineDTO =
        RetailTransactionLineDTO.builder()
            .sourceReferenceTransactionId("1")
            .businessDate(LocalDate.parse("2023-06-15"))
            .transactionDate(LocalDate.parse("2023-04-26"))
            .sourceProcessedDate(LocalDate.parse("2022-12-12"))
            .transactionType("SALE")
            .ringingStore("808")
            .transactionReversalCode("N")
            .sourceReferenceLineType("ItemLine")
            .sourceReferenceLineId("2")
            .transactionLineType("S")
            .storeOfIntent("808")
            .departmentId("802")
            .classId("63")
            .feeCode("151")
            .cashDisbursementLine1("Cash1")
            .cashDisbursementLine2("Cash2")
            .fulfillmentTypeDropshipCode("StoreShipSend")
            .waivedReasonCode("Waived")
            .lineItemAmount(BigDecimal.valueOf(0.0))
            .taxAmount(BigDecimal.valueOf(2.0))
            .employeeDiscountAmount(BigDecimal.valueOf(3.0))
            .waivedAmount(BigDecimal.valueOf(4.0))
            .tenderType("NC")
            .tenderCardType("0")
            .tenderCardSubType("RR")
            .tenderAdjustmentCode("Adj")
            .tenderAmount(BigDecimal.valueOf(64.95))
            .dataSourceCode("RETAIL")
            .build();

    return retailTransactionLineDTO;
  }

  public static Transaction generateTransaction() {
    Transaction transaction = new Transaction();
    String[] record = getDefaultRetailTransaction();

    transaction.setSourceReferenceTransactionId(record[0]);
    transaction.setTransactionDate(LocalDate.parse(record[1]));
    transaction.setBusinessDate(LocalDate.parse(record[2]));
    transaction.setSourceProcessedDate(LocalDate.parse(record[3]));
    transaction.setTransactionType(record[5]);
    transaction.setTransactionReversalCode(record[6]);
    transaction.setSourceReferenceType("retail");
    return transaction;
  }

  public static TransactionLine generateTransactionLine() {
    TransactionLine transactionLine = new TransactionLine();
    String[] record = getDefaultRetailTransaction();

    transactionLine.setRingingStore(record[4]);
    transactionLine.setSourceReferenceLineType(record[7]);
    transactionLine.setSourceReferenceLineId(record[8]);
    transactionLine.setTransactionLineType(record[9]);
    transactionLine.setStoreOfIntent(record[10]);
    return transactionLine;
  }

  public static RetailTransactionLine generateRetailTransaction() {
    RetailTransactionLine retailTransactionLine = new RetailTransactionLine();
    String[] record = getDefaultRetailTransaction();

    retailTransactionLine.setDepartmentId(StringFormatUtility.toFourDigitFormat(record[11]));
    retailTransactionLine.setClassId(StringFormatUtility.toFourDigitFormat(record[12]));
    retailTransactionLine.setFeeCode(StringFormatUtility.toFourDigitFormat(record[13]));
    retailTransactionLine.setCashDisbursementLine1(record[14]);
    retailTransactionLine.setCashDisbursementLine2(record[15]);
    retailTransactionLine.setFulfillmentTypeDropshipCode(record[16]);
    retailTransactionLine.setWaivedReasonCode(record[17]);
    retailTransactionLine.setLineItemAmount(new BigDecimal(record[18]));
    retailTransactionLine.setTaxAmount(new BigDecimal(record[19]));
    retailTransactionLine.setEmployeeDiscountAmount(new BigDecimal(record[20]));
    retailTransactionLine.setWaivedAmount(new BigDecimal(record[21]));
    retailTransactionLine.setTenderType(record[22]);
    retailTransactionLine.setTenderCardTypeCode(record[23]);
    retailTransactionLine.setTenderCardSubTypeCode(record[24]);
    retailTransactionLine.setTenderActivityCode(record[25]);
    retailTransactionLine.setTenderAmount(new BigDecimal(record[26]));

    return retailTransactionLine;
  }

  public static RetailTransactionLine generateFullRetailTransaction() {
    RetailTransactionLine retailTransactionLine = new RetailTransactionLine();
    String[] record = getDefaultRetailTransaction();

    retailTransactionLine.setDepartmentId(record[11]);
    retailTransactionLine.setClassId(record[12]);
    retailTransactionLine.setFeeCode(record[13]);
    retailTransactionLine.setCashDisbursementLine1(record[14]);
    retailTransactionLine.setCashDisbursementLine2(record[15]);
    retailTransactionLine.setFulfillmentTypeDropshipCode(record[16]);
    retailTransactionLine.setWaivedReasonCode(record[17]);
    retailTransactionLine.setLineItemAmount(new BigDecimal(record[18]));
    retailTransactionLine.setTaxAmount(new BigDecimal(record[19]));
    retailTransactionLine.setEmployeeDiscountAmount(new BigDecimal(record[20]));
    retailTransactionLine.setWaivedAmount(new BigDecimal(record[21]));
    retailTransactionLine.setTenderType(record[22]);
    retailTransactionLine.setTenderCardTypeCode(record[23]);
    retailTransactionLine.setTenderCardSubTypeCode(record[24]);
    retailTransactionLine.setTenderActivityCode(record[25]);
    retailTransactionLine.setTenderAmount(new BigDecimal(record[26]));
    TransactionLine transactionLine = RecordGenerator.generateTransactionLine();
    Transaction transaction = RecordGenerator.generateTransaction();
    transaction.setTransactionLines(List.of(transactionLine));
    transactionLine.setTransaction(transaction);
    transactionLine.setRetailTransactionLine(retailTransactionLine);
    retailTransactionLine.setTransactionLine(transactionLine);

    return retailTransactionLine;
  }
}
