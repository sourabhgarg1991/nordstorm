package com.nordstrom.finance.dataintegration.promotion.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.service.PromotionConsumerDBService;
import com.nordstrom.finance.dataintegration.promotion.domain.model.PersistenceResult;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import com.nordstrom.finance.dataintegration.promotion.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.promotion.exception.DatabaseOperationException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionBatchProcessorTest {

  @Mock private PromotionConsumerDBService promotionConsumerDBService;
  @Mock private MetricsClient metricsClient;
  @InjectMocks private PromotionBatchProcessor promotionBatchProcessor;

  private List<TransactionDetailVO> testTransactions;

  @BeforeEach
  void setUp() {
    // Create test data
    testTransactions =
        List.of(
            new TransactionDetailVO(
                List.of(), // lineItems - empty for test
                java.time.LocalDate.now(),
                "GLOBAL_TX_001"),
            new TransactionDetailVO(List.of(), java.time.LocalDate.now(), "GLOBAL_TX_002"));
  }

  @Test
  void testProcessBatchAsync_Success() throws Exception {
    // Given
    int pageNumber = 1;
    int expectedPersisted = 2;
    when(promotionConsumerDBService.processAndPersistPromotionBatch(testTransactions))
        .thenReturn(expectedPersisted);

    // When
    CompletableFuture<PersistenceResult> future =
        promotionBatchProcessor.processBatchAsync(testTransactions, pageNumber);

    // Then
    assertThat(future).isNotNull();
    PersistenceResult result = future.get(); // This blocks until complete

    assertThat(result).isNotNull();
    assertThat(result.pageNumber()).isEqualTo(pageNumber);
    assertThat(result.persisted()).isEqualTo(expectedPersisted);

    verify(promotionConsumerDBService).processAndPersistPromotionBatch(testTransactions);
  }

  @Test
  void testProcessBatchAsync_WithDuplicates() throws Exception {
    // Given
    int pageNumber = 2;
    int expectedPersisted = 1; // Only 1 new record, 1 duplicate
    when(promotionConsumerDBService.processAndPersistPromotionBatch(testTransactions))
        .thenReturn(expectedPersisted);

    // When
    CompletableFuture<PersistenceResult> future =
        promotionBatchProcessor.processBatchAsync(testTransactions, pageNumber);

    // Then
    PersistenceResult result = future.get();
    assertThat(result.persisted()).isEqualTo(expectedPersisted);
    assertThat(result.pageNumber()).isEqualTo(pageNumber);
  }

  @Test
  void testProcessBatchAsync_EmptyList() throws Exception {
    // Given
    List<TransactionDetailVO> emptyList = List.of();
    int pageNumber = 3;
    when(promotionConsumerDBService.processAndPersistPromotionBatch(emptyList)).thenReturn(0);

    // When
    CompletableFuture<PersistenceResult> future =
        promotionBatchProcessor.processBatchAsync(emptyList, pageNumber);

    // Then
    PersistenceResult result = future.get();
    assertThat(result.persisted()).isEqualTo(0);
    assertThat(result.pageNumber()).isEqualTo(pageNumber);
  }

  @Test
  void testProcessBatchAsync_DatabaseException()
      throws DatabaseOperationException, DatabaseConnectionException {
    // Given
    int pageNumber = 4;
    DatabaseOperationException dbException = new DatabaseOperationException("Failed to persist");
    when(promotionConsumerDBService.processAndPersistPromotionBatch(anyList()))
        .thenThrow(dbException);

    // When
    CompletableFuture<PersistenceResult> future =
        promotionBatchProcessor.processBatchAsync(testTransactions, pageNumber);

    // Then
    assertThat(future).isCompletedExceptionally();
    assertThat(future.isCompletedExceptionally()).isTrue();

    // Verify the exception
    ExecutionException executionException =
        org.junit.jupiter.api.Assertions.assertThrows(ExecutionException.class, future::get);
    assertThat(executionException.getCause()).isEqualTo(dbException);
  }

  @Test
  void testProcessBatchAsync_RuntimeException()
      throws DatabaseOperationException, DatabaseConnectionException {
    // Given
    int pageNumber = 5;
    RuntimeException runtimeException = new RuntimeException("Unexpected error");
    when(promotionConsumerDBService.processAndPersistPromotionBatch(anyList()))
        .thenThrow(runtimeException);

    // When
    CompletableFuture<PersistenceResult> future =
        promotionBatchProcessor.processBatchAsync(testTransactions, pageNumber);

    // Then
    assertThat(future.isCompletedExceptionally()).isTrue();

    ExecutionException executionException =
        org.junit.jupiter.api.Assertions.assertThrows(ExecutionException.class, future::get);
    assertThat(executionException.getCause()).isEqualTo(runtimeException);
  }

  @Test
  void testProcessBatchAsync_LargeBatch() throws Exception {
    // Given
    List<TransactionDetailVO> largeBatch =
        java.util.stream.IntStream.range(0, 2000)
            .mapToObj(
                i ->
                    new TransactionDetailVO(List.of(), java.time.LocalDate.now(), "GLOBAL_TX_" + i))
            .toList();
    int pageNumber = 10;
    int expectedPersisted = 1950; // Some duplicates

    when(promotionConsumerDBService.processAndPersistPromotionBatch(largeBatch))
        .thenReturn(expectedPersisted);

    // When
    CompletableFuture<PersistenceResult> future =
        promotionBatchProcessor.processBatchAsync(largeBatch, pageNumber);

    // Then
    PersistenceResult result = future.get();
    assertThat(result.persisted()).isEqualTo(expectedPersisted);
    assertThat(result.pageNumber()).isEqualTo(pageNumber);
  }
}
