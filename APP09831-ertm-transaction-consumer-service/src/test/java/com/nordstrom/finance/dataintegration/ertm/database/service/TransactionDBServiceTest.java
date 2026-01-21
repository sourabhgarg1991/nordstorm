package com.nordstrom.finance.dataintegration.ertm.database.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.repository.TransactionRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class TransactionDBServiceTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private MetricsClient metricsClient;
  @InjectMocks private TransactionDBService transactionDBService;

  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @Test
  void testIsDuplicateTransaction() {
    String transactionId = "TXN123";
    String lineId = "LINE123";
    when(transactionRepository.getExistingLineItemIds(transactionId, List.of(lineId)))
        .thenReturn(List.of(lineId));

    List<String> result =
        transactionDBService.getExistingLineItemIds(transactionId, List.of(lineId));

    assertTrue(result.contains(lineId));
  }

  @Test
  void testSaveTransaction_successfulSave() throws Exception {
    Transaction transaction = new Transaction();
    when(transactionRepository.getExistingLineItemIds(any(), any()))
        .thenReturn(Collections.emptyList());
    when(transactionRepository.save(transaction)).thenReturn(transaction);

    assertDoesNotThrow(() -> transactionDBService.saveTransaction(transaction));
    verify(transactionRepository, times(1)).save(transaction);
  }

  @Test
  void testSaveTransaction_duplicate_skipsSave() throws Exception {
    when(transactionRepository.getExistingLineItemIds(any(), any())).thenReturn(List.of("LINE123"));

    List<String> result = transactionDBService.getExistingLineItemIds("TXN123", List.of("LINE123"));
    assertTrue(result.contains("LINE123"));
  }

  @Test
  void testGetAll_returnsTransactions() {
    Transaction transaction = new Transaction();
    when(transactionRepository.findAll()).thenReturn(List.of(transaction));

    List<Transaction> result = transactionDBService.getAll();
    assertTrue(result.size() > 0);
    assertEquals(transaction, result.get(0));
  }

  @Test
  void testSaveAllTransaction_invokesRepository() {
    Transaction transaction = new Transaction();
    transactionDBService.saveAllTransaction(List.of(transaction));
    verify(transactionRepository, times(1)).saveAll(any());
  }
}
