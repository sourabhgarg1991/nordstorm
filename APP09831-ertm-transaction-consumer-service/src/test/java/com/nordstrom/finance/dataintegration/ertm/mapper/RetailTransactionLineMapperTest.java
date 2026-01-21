package com.nordstrom.finance.dataintegration.ertm.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.RetailTransactionLine;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.entity.TransactionLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {RetailTransactionLineMapper.class})
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class RetailTransactionLineMapperTest {

  @MockitoBean private TransactionLineMapper transactionLineMapper;
  @MockitoBean private MetricsClient metricsClient;

  @InjectMocks @Autowired RetailTransactionLineMapper retailTransactionLineMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void retailTransactionLineMapperTest_success() {

    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();
    when(transactionLineMapper.mapRecordToTransactionLine(any(), any(Transaction.class)))
        .thenReturn(Mockito.mock(TransactionLine.class));

    RetailTransactionLine actualTransaction =
        retailTransactionLineMapper.mapRecordToRetailTransactionLine(
            record, Mockito.mock(Transaction.class));
    RetailTransactionLine expectedTransaction = RecordGenerator.generateRetailTransaction();

    assertEquals(expectedTransaction.getDepartmentId(), actualTransaction.getDepartmentId());
    assertEquals(expectedTransaction.getClassId(), actualTransaction.getClassId());
    assertEquals(expectedTransaction.getFeeCode(), actualTransaction.getFeeCode());
    assertEquals(
        expectedTransaction.getCashDisbursementLine1(),
        actualTransaction.getCashDisbursementLine1());
    assertEquals(
        expectedTransaction.getCashDisbursementLine2(),
        actualTransaction.getCashDisbursementLine2());
    assertEquals(
        expectedTransaction.getFulfillmentTypeDropshipCode(),
        actualTransaction.getFulfillmentTypeDropshipCode());
    assertEquals(
        expectedTransaction.getWaivedReasonCode(), actualTransaction.getWaivedReasonCode());
    assertEquals(expectedTransaction.getLineItemAmount(), actualTransaction.getLineItemAmount());
    assertEquals(expectedTransaction.getTaxAmount(), actualTransaction.getTaxAmount());
    assertEquals(
        expectedTransaction.getEmployeeDiscountAmount(),
        actualTransaction.getEmployeeDiscountAmount());
    assertEquals(expectedTransaction.getWaivedAmount(), actualTransaction.getWaivedAmount());
    assertEquals(expectedTransaction.getTenderType(), actualTransaction.getTenderType());
    assertEquals(
        expectedTransaction.getTenderCardTypeCode(), actualTransaction.getTenderCardTypeCode());
    assertEquals(
        expectedTransaction.getTenderCardSubTypeCode(),
        actualTransaction.getTenderCardSubTypeCode());
    assertEquals(
        expectedTransaction.getTenderActivityCode(), actualTransaction.getTenderActivityCode());
    assertEquals(expectedTransaction.getTenderAmount(), actualTransaction.getTenderAmount());
  }

  @Test
  public void retailTransactionLineMapperTest_negativeAmountCheck() {

    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();
    record.setLineItemAmount(record.getLineItemAmount().negate());
    record.setEmployeeDiscountAmount(record.getEmployeeDiscountAmount().negate());
    record.setTaxAmount(record.getTaxAmount().negate());
    record.setTenderAmount(record.getTenderAmount().negate());
    record.setWaivedAmount(record.getWaivedAmount().negate());
    when(transactionLineMapper.mapRecordToTransactionLine(any(), any(Transaction.class)))
        .thenReturn(Mockito.mock(TransactionLine.class));

    RetailTransactionLine actualTransaction =
        retailTransactionLineMapper.mapRecordToRetailTransactionLine(
            record, Mockito.mock(Transaction.class));
    RetailTransactionLine expectedTransaction = RecordGenerator.generateRetailTransaction();

    assertEquals(expectedTransaction.getDepartmentId(), actualTransaction.getDepartmentId());
    assertEquals(expectedTransaction.getClassId(), actualTransaction.getClassId());
    assertEquals(expectedTransaction.getFeeCode(), actualTransaction.getFeeCode());
    assertEquals(
        expectedTransaction.getCashDisbursementLine1(),
        actualTransaction.getCashDisbursementLine1());
    assertEquals(
        expectedTransaction.getCashDisbursementLine2(),
        actualTransaction.getCashDisbursementLine2());
    assertEquals(
        expectedTransaction.getFulfillmentTypeDropshipCode(),
        actualTransaction.getFulfillmentTypeDropshipCode());
    assertEquals(
        expectedTransaction.getWaivedReasonCode(), actualTransaction.getWaivedReasonCode());
    assertEquals(
        expectedTransaction.getLineItemAmount().negate(), actualTransaction.getLineItemAmount());
    assertEquals(expectedTransaction.getTaxAmount().negate(), actualTransaction.getTaxAmount());
    assertEquals(
        expectedTransaction.getEmployeeDiscountAmount().negate(),
        actualTransaction.getEmployeeDiscountAmount());
    assertEquals(
        expectedTransaction.getWaivedAmount().negate(), actualTransaction.getWaivedAmount());
    assertEquals(expectedTransaction.getTenderType(), actualTransaction.getTenderType());
    assertEquals(
        expectedTransaction.getTenderCardTypeCode(), actualTransaction.getTenderCardTypeCode());
    assertEquals(
        expectedTransaction.getTenderCardSubTypeCode(),
        actualTransaction.getTenderCardSubTypeCode());
    assertEquals(
        expectedTransaction.getTenderActivityCode(), actualTransaction.getTenderActivityCode());
    assertEquals(
        expectedTransaction.getTenderAmount().negate(), actualTransaction.getTenderAmount());
  }
}
