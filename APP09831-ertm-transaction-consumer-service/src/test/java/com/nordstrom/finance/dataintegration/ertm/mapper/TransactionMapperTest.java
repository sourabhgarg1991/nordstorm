package com.nordstrom.finance.dataintegration.ertm.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {TransactionMapper.class})
@ActiveProfiles("test")
public class TransactionMapperTest {

  @Autowired private TransactionMapper transactionMapper;
  @MockitoBean private MetricsClient metricsClient;

  @Test
  public void transactionMapperTest_success() {
    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();

    Transaction actualTransaction = transactionMapper.mapRecordToTransaction(record);
    Transaction expectedTransaction = RecordGenerator.generateTransaction();

    assertEquals(
        expectedTransaction.getSourceReferenceTransactionId(),
        actualTransaction.getSourceReferenceTransactionId());
    assertEquals(expectedTransaction.getTransactionDate(), actualTransaction.getTransactionDate());
    assertEquals(expectedTransaction.getBusinessDate(), actualTransaction.getBusinessDate());
    assertEquals(
        expectedTransaction.getSourceProcessedDate(), actualTransaction.getSourceProcessedDate());
    assertEquals(expectedTransaction.getTransactionType(), actualTransaction.getTransactionType());
    assertEquals(
        expectedTransaction.getTransactionReversalCode(),
        actualTransaction.getTransactionReversalCode());
    assertEquals(
        expectedTransaction.getSourceReferenceType(), actualTransaction.getSourceReferenceType());
  }

  @Test
  public void transactionMapperTest_dataSourceCode_null() {
    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();
    record.setDataSourceCode(null);
    Transaction actualTransaction = transactionMapper.mapRecordToTransaction(record);
    Transaction expectedTransaction = RecordGenerator.generateTransaction();

    assertEquals(
        expectedTransaction.getSourceReferenceTransactionId(),
        actualTransaction.getSourceReferenceTransactionId());
    assertEquals(expectedTransaction.getTransactionDate(), actualTransaction.getTransactionDate());
    assertEquals(expectedTransaction.getBusinessDate(), actualTransaction.getBusinessDate());
    assertEquals(
        expectedTransaction.getSourceProcessedDate(), actualTransaction.getSourceProcessedDate());
    assertEquals(expectedTransaction.getTransactionType(), actualTransaction.getTransactionType());
    assertEquals(
        expectedTransaction.getTransactionReversalCode(),
        actualTransaction.getTransactionReversalCode());
    assertEquals("retail", actualTransaction.getSourceReferenceType());
  }

  @Test
  public void transactionMapperTest_dataSourceCode_empty() {
    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();
    record.setDataSourceCode("");
    Transaction actualTransaction = transactionMapper.mapRecordToTransaction(record);
    Transaction expectedTransaction = RecordGenerator.generateTransaction();

    assertEquals(
        expectedTransaction.getSourceReferenceTransactionId(),
        actualTransaction.getSourceReferenceTransactionId());
    assertEquals(expectedTransaction.getTransactionDate(), actualTransaction.getTransactionDate());
    assertEquals(expectedTransaction.getBusinessDate(), actualTransaction.getBusinessDate());
    assertEquals(
        expectedTransaction.getSourceProcessedDate(), actualTransaction.getSourceProcessedDate());
    assertEquals(expectedTransaction.getTransactionType(), actualTransaction.getTransactionType());
    assertEquals(
        expectedTransaction.getTransactionReversalCode(),
        actualTransaction.getTransactionReversalCode());
    assertEquals("retail", actualTransaction.getSourceReferenceType());
  }
}
