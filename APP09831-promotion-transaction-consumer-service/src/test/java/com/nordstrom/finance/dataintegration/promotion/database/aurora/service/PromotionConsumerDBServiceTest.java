package com.nordstrom.finance.dataintegration.promotion.database.aurora.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.Transaction;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.repository.TransactionRepository;
import com.nordstrom.finance.dataintegration.promotion.domain.mapper.PromotionEntityMapper;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import com.nordstrom.finance.dataintegration.promotion.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.promotion.exception.DatabaseOperationException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PromotionConsumerDBServiceTest {

  @Mock TransactionRepository transactionRepository;
  @Mock MetricsClient metricsClient;

  @InjectMocks PromotionConsumerDBService service;

  PromotionConsumerDBServiceTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void processAndPersistPromotionBatch_emptyList_returnsEarly() throws Exception {
    int result = service.processAndPersistPromotionBatch(Collections.emptyList());
    assertEquals(0, result);
    verifyNoInteractions(transactionRepository);
  }

  @Test
  void processAndPersistPromotionBatch_allDuplicates_skipsPersistence() throws Exception {
    TransactionDetailVO vo = mock(TransactionDetailVO.class);
    when(vo.globalTransactionId()).thenReturn("id1");
    when(transactionRepository.findExistingSourceReferenceTransactionIds(anyList()))
        .thenReturn(Set.of("id1"));

    int result = service.processAndPersistPromotionBatch(List.of(vo));
    assertEquals(0, result);
    verify(transactionRepository, never()).saveAll(anyList());
  }

  @Test
  void processAndPersistPromotionBatch_newTransactions_persistsEntities() throws Exception {
    TransactionDetailVO vo = mock(TransactionDetailVO.class);
    when(vo.globalTransactionId()).thenReturn("id2");
    when(transactionRepository.findExistingSourceReferenceTransactionIds(anyList()))
        .thenReturn(Set.of());
    List<Transaction> entities = List.of(mock(Transaction.class));
    try (var mocked = mockStatic(PromotionEntityMapper.class)) {
      mocked.when(() -> PromotionEntityMapper.mapToTransactions(anyList())).thenReturn(entities);
      when(transactionRepository.saveAll(entities)).thenReturn(entities);

      int result = service.processAndPersistPromotionBatch(List.of(vo));
      assertEquals(entities.size(), result);
      verify(transactionRepository).saveAll(entities);
    }
  }

  @Test
  void processAndPersistPromotionBatch_jdbcConnectionException_throwsDatabaseConnectionException() {
    TransactionDetailVO vo = mock(TransactionDetailVO.class);
    when(vo.globalTransactionId()).thenReturn("id3");
    when(transactionRepository.findExistingSourceReferenceTransactionIds(anyList()))
        .thenThrow(new JDBCConnectionException("Connection error", new SQLException()));

    assertThrows(
        DatabaseConnectionException.class,
        () -> service.processAndPersistPromotionBatch(List.of(vo)));
  }

  @Test
  void processAndPersistPromotionBatch_queryTimeoutException_throwsDatabaseConnectionException() {
    TransactionDetailVO vo = mock(TransactionDetailVO.class);
    when(vo.globalTransactionId()).thenReturn("id4");
    when(transactionRepository.findExistingSourceReferenceTransactionIds(anyList()))
        .thenThrow(new QueryTimeoutException("Timeout", new SQLException(), "sql"));

    assertThrows(
        DatabaseConnectionException.class,
        () -> service.processAndPersistPromotionBatch(List.of(vo)));
  }

  @Test
  void processAndPersistPromotionBatch_runtimeException_throwsDatabaseOperationException() {
    TransactionDetailVO vo = mock(TransactionDetailVO.class);
    when(vo.globalTransactionId()).thenReturn("id5");
    when(transactionRepository.findExistingSourceReferenceTransactionIds(anyList()))
        .thenThrow(new RuntimeException("Unexpected"));

    assertThrows(
        DatabaseOperationException.class,
        () -> service.processAndPersistPromotionBatch(List.of(vo)));
  }
}
