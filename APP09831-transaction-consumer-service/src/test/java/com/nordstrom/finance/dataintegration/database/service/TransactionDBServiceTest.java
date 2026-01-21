package com.nordstrom.finance.dataintegration.database.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.repository.TransactionRepository;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.metric.Metric;
import com.nordstrom.finance.dataintegration.metric.MetricErrorCode;
import java.util.Optional;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TransactionDBServiceTest {

  @Mock private MetricsClient metricsClient;
  @Mock private TransactionRepository transactionRepository;
  @InjectMocks private TransactionDBService transactionService;

  private Transaction sampleTransaction() {
    return Transaction.builder()
        .transactionId(1L)
        .sourceReferenceTransactionId("SRC_TXN_ID")
        .sourceReferenceSystemType("SYSTEM")
        .sourceReferenceType("LOB")
        .sourceProcessedDate(java.time.LocalDate.now())
        .transactionDate(java.time.LocalDate.now())
        .businessDate(java.time.LocalDate.now())
        .transactionType("SALE")
        .transactionReversalCode("REV")
        .build();
  }

  @Test
  void testSaveTransaction_success() {
    Transaction transaction = sampleTransaction();

    when(transactionRepository.save(transaction)).thenReturn(transaction);

    transactionService.saveTransaction(transaction);

    verify(transactionRepository, times(1)).save(transaction);
    verify(metricsClient, times(1)).incrementCounter(Metric.SAVE_TRANSACTION_COUNT.getMetricName());
    verify(metricsClient, times(1))
        .recordExecutionTime(eq(Metric.SAVE_TRANSACTION_TIME.getMetricName()), anyLong());
  }

  @Test
  void testSaveTransaction_databaseConnectionException() {
    Transaction transaction = sampleTransaction();

    when(transactionRepository.save(transaction))
        .thenThrow(new JDBCConnectionException("db conn failure", new java.sql.SQLException()));

    assertThrows(
        DatabaseConnectionException.class, () -> transactionService.saveTransaction(transaction));

    verify(transactionRepository, times(1)).save(transaction);
    verify(metricsClient, times(1))
        .incrementErrorCount(
            MetricsCommonTag.ERROR_CODE.getTag(
                (MetricErrorCode.SAVE_TRANSACTIONS_ERROR_COUNT.getErrorValue())));
  }

  @Test
  void testSaveTransaction_databaseOperationException() {
    Transaction transaction = sampleTransaction();

    when(transactionRepository.save(transaction)).thenThrow(new RuntimeException("boom"));

    assertThrows(
        DatabaseOperationException.class, () -> transactionService.saveTransaction(transaction));

    verify(transactionRepository, times(1)).save(transaction);
    verify(metricsClient, times(1))
        .incrementErrorCount(
            MetricsCommonTag.ERROR_CODE.getTag(
                (MetricErrorCode.SAVE_TRANSACTIONS_ERROR_COUNT.getErrorValue())));
  }

  @Test
  void testFindById() {
    Long id = 1L;
    Transaction transaction = sampleTransaction();

    when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));

    Optional<Transaction> result = transactionService.findById(id);

    assertTrue(result.isPresent());
    assertEquals(transaction, result.get());
    verify(transactionRepository, times(1)).findById(id);
  }

  @Test
  void testDeleteById() {
    Long id = 1L;

    doNothing().when(transactionRepository).deleteById(id);

    transactionService.deleteById(id);

    verify(transactionRepository).deleteById(id);
  }

  @Test
  void testExistsByTransactionId_true() {
    when(transactionRepository.existsByTransactionId("SYSTEM", "LOB", "SRC_TXN_ID"))
        .thenReturn(true);
    boolean exists = transactionService.existsByTransactionId("SYSTEM", "LOB", "SRC_TXN_ID");
    assertTrue(exists);
    verify(transactionRepository, times(1)).existsByTransactionId("SYSTEM", "LOB", "SRC_TXN_ID");
  }

  @Test
  void testExistsByTransactionId_false() {
    when(transactionRepository.existsByTransactionId("SYSTEM", "LOB", "SRC_TXN_ID"))
        .thenReturn(false);
    boolean exists = transactionService.existsByTransactionId("SYSTEM", "LOB", "SRC_TXN_ID");
    assertFalse(exists);
    verify(transactionRepository, times(1)).existsByTransactionId("SYSTEM", "LOB", "SRC_TXN_ID");
  }
}
