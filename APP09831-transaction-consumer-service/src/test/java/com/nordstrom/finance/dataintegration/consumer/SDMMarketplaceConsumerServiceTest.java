package com.nordstrom.finance.dataintegration.consumer;

import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_MARKETPLACE;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.EVENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.KafkaNonRetryableException;
import com.nordstrom.finance.dataintegration.exception.KafkaRetryableException;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortknoxException;
import com.nordstrom.finance.dataintegration.mapper.FinancialRetailTransactionMapper;
import com.nordstrom.finance.dataintegration.metric.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.MDC;

class SDMMarketplaceConsumerServiceTest {

  @Mock private MetricsClient metricsClient;
  @Mock private TransactionDBService transactionDBService;
  @Mock private FinancialRetailTransactionMapper financialRetailTransactionMapper;
  @InjectMocks private SDMMarketplaceConsumerService consumerService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    MDC.clear();
  }

  @Test
  void testSetLogContext_setsMDCValues() {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("100");

    consumerService.consumeSDMEvents(txn, 5, 2);

    assertEquals("MARKETPLACE", MDC.get("SOURCE_REFERENCE_TYPE"));
    assertEquals("100", MDC.get("SDM_ID"));
    assertEquals("5", MDC.get("OFFSET"));
    assertEquals("2", MDC.get("PARTITION"));
  }

  @Test
  void testProcessMarketplaceTransaction_skipsWhenExists() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("201");
    when(transactionDBService.existsByTransactionId(any(), any(), any())).thenReturn(true);

    consumerService.processMarketplaceTransaction(txn);

    verify(transactionDBService, never()).saveTransaction(any());
    verify(metricsClient, times(1))
        .incrementCounter(
            Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testProcessMarketplaceTransaction_savesWhenNotExists() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("200");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doNothing().when(transactionDBService).saveTransaction(any());

    consumerService.processMarketplaceTransaction(txn);

    verify(transactionDBService, times(1)).saveTransaction(mapped);
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testProcessMarketplaceTransaction_throwsKafkaRetryableException() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("202");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doThrow(new DatabaseConnectionException("DB connection fail"))
        .when(transactionDBService)
        .saveTransaction(any());

    assertThrows(
        KafkaRetryableException.class, () -> consumerService.processMarketplaceTransaction(txn));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testProcessMarketplaceTransaction_throwsKafkaNonRetryableException()
      throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("203");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doThrow(new RuntimeException("Other error")).when(transactionDBService).saveTransaction(any());

    assertThrows(
        KafkaNonRetryableException.class, () -> consumerService.processMarketplaceTransaction(txn));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testProcessMarketplaceTransaction_mapperThrowsException() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("204");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    lenient()
        .when(financialRetailTransactionMapper.toTransaction(txn))
        .thenThrow(new RuntimeException("Mapping error"));

    assertThrows(
        KafkaNonRetryableException.class, () -> consumerService.processMarketplaceTransaction(txn));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testProcessMarketplaceTransaction_mapperThrowsFortknoxException() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("205");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    lenient()
        .when(financialRetailTransactionMapper.toTransaction(txn))
        .thenThrow(new FortknoxException("Fortknox error"));

    assertThrows(
        KafkaNonRetryableException.class, () -> consumerService.processMarketplaceTransaction(txn));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testConsumeSDMEvents_successfulProcessing() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("100");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doNothing().when(transactionDBService).saveTransaction(any());

    consumerService.consumeSDMEvents(txn, 10, 1);

    verify(transactionDBService, times(1)).saveTransaction(mapped);
    verify(metricsClient, times(1))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testConsumeSDMEvents_duplicateSkipped() {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("101");
    when(transactionDBService.existsByTransactionId(any(), any(), any())).thenReturn(true);

    consumerService.consumeSDMEvents(txn, 10, 1);

    verify(transactionDBService, never()).saveTransaction(any());
    verify(metricsClient, times(1))
        .incrementCounter(
            Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testConsumeSDMEvents_catchesAndRethrowsKafkaRetryableException() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("102");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doThrow(new DatabaseConnectionException("DB error"))
        .when(transactionDBService)
        .saveTransaction(any());

    assertThrows(KafkaRetryableException.class, () -> consumerService.consumeSDMEvents(txn, 10, 1));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testConsumeSDMEvents_catchesAndRethrowsKafkaNonRetryableException()
      throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("103");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doThrow(new RuntimeException("Other error")).when(transactionDBService).saveTransaction(any());

    assertThrows(
        KafkaNonRetryableException.class, () -> consumerService.consumeSDMEvents(txn, 10, 1));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testConsumeSDMEvents_wrapsUnexpectedExceptionAsNonRetryable() {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("104");
    when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Unexpected error"));

    KafkaNonRetryableException thrown =
        assertThrows(
            KafkaNonRetryableException.class, () -> consumerService.consumeSDMEvents(txn, 10, 1));

    assertTrue(thrown.getMessage().contains("Unexpected error"));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testConsumeSDMEvents_rethrowsKafkaRetryableExceptionAsIs() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("106");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doThrow(new DatabaseConnectionException("DB connection error"))
        .when(transactionDBService)
        .saveTransaction(any());

    KafkaRetryableException exception =
        assertThrows(
            KafkaRetryableException.class, () -> consumerService.consumeSDMEvents(txn, 10, 1));

    assertTrue(exception.getMessage().contains("Retryable issue occurred"));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testConsumeSDMEvents_rethrowsKafkaNonRetryableExceptionAsIs() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("107");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doThrow(new IllegalStateException("Non-retryable error"))
        .when(transactionDBService)
        .saveTransaction(any());

    KafkaNonRetryableException exception =
        assertThrows(
            KafkaNonRetryableException.class, () -> consumerService.consumeSDMEvents(txn, 10, 1));

    assertTrue(exception.getMessage().contains("Non-retryable issue occurred"));
    verify(metricsClient, times(0))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }

  @Test
  void testGetCurrentTimestamp() throws FortknoxException {
    FinancialRetailTransaction txn = mock(FinancialRetailTransaction.class);
    when(txn.getFinancialRetailTransactionRecordId()).thenReturn("105");
    lenient()
        .when(transactionDBService.existsByTransactionId(any(), any(), any()))
        .thenReturn(false);

    Transaction mapped = mock(Transaction.class);
    lenient().when(financialRetailTransactionMapper.toTransaction(txn)).thenReturn(mapped);
    doNothing().when(transactionDBService).saveTransaction(any());

    assertDoesNotThrow(() -> consumerService.consumeSDMEvents(txn, 10, 1));
    verify(metricsClient, times(1))
        .incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
  }
}
