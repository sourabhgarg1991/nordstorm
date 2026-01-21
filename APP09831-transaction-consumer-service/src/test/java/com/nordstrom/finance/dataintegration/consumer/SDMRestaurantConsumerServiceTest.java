package com.nordstrom.finance.dataintegration.consumer;

import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_RESTAURANT;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.EVENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.KafkaNonRetryableException;
import com.nordstrom.finance.dataintegration.exception.KafkaRetryableException;
import com.nordstrom.finance.dataintegration.mapper.FinancialRestaurantTransactionMapper;
import com.nordstrom.finance.dataintegration.metric.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.MDC;

class SDMRestaurantConsumerServiceTest {

  @Mock private MetricsClient metricsClient;
  @Mock private TransactionDBService transactionDBService;
  @Mock private FinancialRestaurantTransactionMapper financialRestaurantTransactionMapper;
  @InjectMocks private SDMRestaurantConsumerService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    MDC.clear();
  }

  @Test
  void testConsumeSDMRestaurantEvents_duplicateTransaction_skipsProcessing() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(true);

    service.consumeSDMRestaurantEvents(tx, 1, 0);

    verify(transactionDBService, never()).saveTransaction(any());
    assertEquals("RESTAURANT", MDC.get("SOURCE_REFERENCE_TYPE"));
    assertEquals("123", MDC.get("SDM_ID"));
    verify(metricsClient, times(1))
        .incrementCounter(
            Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testConsumeSDMRestaurantEvents_successfulProcessing() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doNothing().when(transactionDBService).saveTransaction(any());

    service.consumeSDMRestaurantEvents(tx, 1, 0);

    verify(transactionDBService, times(1)).saveTransaction(transaction);
    verify(metricsClient, times(1))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testConsumeSDMRestaurantEvents_catchesAndRethrowsKafkaRetryableException() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doThrow(new DatabaseConnectionException("DB error"))
        .when(transactionDBService)
        .saveTransaction(any());

    assertThrows(KafkaRetryableException.class, () -> service.consumeSDMRestaurantEvents(tx, 1, 0));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testConsumeSDMRestaurantEvents_catchesAndRethrowsKafkaNonRetryableException() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doThrow(new RuntimeException("Other error")).when(transactionDBService).saveTransaction(any());

    assertThrows(
        KafkaNonRetryableException.class, () -> service.consumeSDMRestaurantEvents(tx, 1, 0));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testConsumeSDMRestaurantEvents_wrapsUnexpectedExceptionAsNonRetryable() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("Unexpected error"));

    KafkaNonRetryableException thrown =
        assertThrows(
            KafkaNonRetryableException.class, () -> service.consumeSDMRestaurantEvents(tx, 1, 0));

    assertTrue(thrown.getMessage().contains("Unexpected error"));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testConsumeSDMRestaurantEvents_rethrowsKafkaRetryableExceptionAsIs() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("124");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doThrow(new DatabaseConnectionException("DB connection error"))
        .when(transactionDBService)
        .saveTransaction(any());

    KafkaRetryableException exception =
        assertThrows(
            KafkaRetryableException.class, () -> service.consumeSDMRestaurantEvents(tx, 1, 0));

    assertTrue(exception.getMessage().contains("Retryable issue occurred"));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testConsumeSDMRestaurantEvents_rethrowsKafkaNonRetryableExceptionAsIs() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("125");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doThrow(new IllegalStateException("Non-retryable error"))
        .when(transactionDBService)
        .saveTransaction(any());

    KafkaNonRetryableException exception =
        assertThrows(
            KafkaNonRetryableException.class, () -> service.consumeSDMRestaurantEvents(tx, 1, 0));

    assertTrue(exception.getMessage().contains("Non-retryable issue occurred"));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testProcessRestaurantTransaction_duplicateSkipped() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(true);

    service.processRestaurantTransaction(tx);

    verify(transactionDBService, never()).saveTransaction(any());
    verify(metricsClient, times(1))
        .incrementCounter(
            Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testProcessRestaurantTransaction_successfulSave() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doNothing().when(transactionDBService).saveTransaction(any());

    service.processRestaurantTransaction(tx);

    verify(transactionDBService, times(1)).saveTransaction(transaction);
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testProcessRestaurantTransaction_throwsKafkaRetryableException() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doThrow(new DatabaseConnectionException("DB error"))
        .when(transactionDBService)
        .saveTransaction(any());

    assertThrows(KafkaRetryableException.class, () -> service.processRestaurantTransaction(tx));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testProcessRestaurantTransaction_throwsKafkaNonRetryableException() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Transaction transaction = mock(Transaction.class);
    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenReturn(transaction);
    doThrow(new RuntimeException("Other error")).when(transactionDBService).saveTransaction(any());

    assertThrows(KafkaNonRetryableException.class, () -> service.processRestaurantTransaction(tx));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testProcessRestaurantTransaction_mapperThrowsException() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-123");
    when(transactionDBService.existsByTransactionId(anyString(), anyString(), anyString()))
        .thenReturn(false);

    when(financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(any()))
        .thenThrow(new RuntimeException("Mapping error"));

    assertThrows(KafkaNonRetryableException.class, () -> service.processRestaurantTransaction(tx));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
  }

  @Test
  void testSetLogContext() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("999");

    service.consumeSDMRestaurantEvents(tx, 5, 2);

    assertEquals("RESTAURANT", MDC.get("SOURCE_REFERENCE_TYPE"));
    assertEquals("999", MDC.get("SDM_ID"));
    assertEquals("5", MDC.get("OFFSET"));
    assertEquals("2", MDC.get("PARTITION"));
  }
}
