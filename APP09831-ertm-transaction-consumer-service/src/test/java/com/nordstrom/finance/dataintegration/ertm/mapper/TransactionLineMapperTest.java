package com.nordstrom.finance.dataintegration.ertm.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.util.StringFormatUtility;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.entity.TransactionLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {TransactionLineMapper.class})
@ActiveProfiles("test")
public class TransactionLineMapperTest {

  @Autowired private TransactionLineMapper transactionLineMapper;
  @MockitoBean private TransactionMapper transactionMapper;
  @MockitoBean private MetricsClient metricsClient;

  @Test
  public void transactionLineMapperTest_success() {
    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();

    Mockito.when(transactionMapper.mapRecordToTransaction(record))
        .thenReturn(Mockito.mock(Transaction.class));
    TransactionLine actualTransaction =
        transactionLineMapper.mapRecordToTransactionLine(record, Mockito.mock(Transaction.class));

    assertEquals(
        StringFormatUtility.toFourDigitFormat(record.getRingingStore()),
        actualTransaction.getRingingStore());
    assertEquals(
        record.getSourceReferenceLineType(), actualTransaction.getSourceReferenceLineType());
    assertEquals(record.getSourceReferenceLineId(), actualTransaction.getSourceReferenceLineId());

    assertEquals(
        "SALE",
        actualTransaction.getTransactionLineType(),
        "transactionLineType should be transformed from 'S' to 'SALE'");

    assertEquals(
        StringFormatUtility.toFourDigitFormat(record.getStoreOfIntent()),
        actualTransaction.getStoreOfIntent());
  }

  @ParameterizedTest
  @CsvSource({
    "S, SALE",
    "s, SALE",
    "R, RETN",
    "r, RETN",
    "R   , RETN",
    "   R, RETN",
    "RETURN, RETN",
    "return, RETN",
    "SALE, SALE",
    "RETN, RETN",
    "sale, SALE",
    "retn, RETN"
  })
  public void testTransactionLineTypeTransformation(String sourceValue, String expectedValue) {
    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();
    record.setTransactionLineType(sourceValue);

    Mockito.when(transactionMapper.mapRecordToTransaction(record))
        .thenReturn(Mockito.mock(Transaction.class));

    TransactionLine result =
        transactionLineMapper.mapRecordToTransactionLine(record, Mockito.mock(Transaction.class));

    assertEquals(
        expectedValue,
        result.getTransactionLineType(),
        String.format("Input '%s' should be transformed to '%s'", sourceValue, expectedValue));
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void testTransactionLineTypeTransformation_NullAndEmpty(String sourceValue) {
    RetailTransactionLineDTO record = RecordGenerator.generateRetailTransactionLineDto();
    record.setTransactionLineType(sourceValue);

    Mockito.when(transactionMapper.mapRecordToTransaction(record))
        .thenReturn(Mockito.mock(Transaction.class));

    TransactionLine result =
        transactionLineMapper.mapRecordToTransactionLine(record, Mockito.mock(Transaction.class));

    assertNull(result.getTransactionLineType(), "Null or empty source value should result in null");
  }
}
