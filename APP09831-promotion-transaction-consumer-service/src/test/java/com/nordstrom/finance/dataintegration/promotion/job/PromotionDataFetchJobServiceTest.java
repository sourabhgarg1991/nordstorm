package com.nordstrom.finance.dataintegration.promotion.job;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.Job;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageRequest;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageResponse;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.service.PromotionGcpQueryService;
import com.nordstrom.finance.dataintegration.promotion.domain.model.PersistenceResult;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionDataFetchJobServiceTest {

  @Mock private PromotionGcpQueryService promotionGcpQueryService;
  @Mock private PromotionBatchProcessor batchProcessor;
  @Mock private MetricsClient metricsClient;

  @InjectMocks private PromotionDataFetchJobService service;

  @Test
  void runWithSinglePageSuccess() throws Exception {
    Job mockJob = mock(Job.class);
    TransactionDetailVO transaction =
        new TransactionDetailVO(Collections.emptyList(), LocalDate.now(), "txn123");
    List<TransactionDetailVO> transactions = List.of(transaction);

    TransactionPageResponse response =
        new TransactionPageResponse(mockJob, transactions, null); // null = no next page

    when(promotionGcpQueryService.getPromotionTransactionData(any(TransactionPageRequest.class)))
        .thenReturn(response);

    PersistenceResult persistenceResult = new PersistenceResult(1, 1);
    CompletableFuture<PersistenceResult> future =
        CompletableFuture.completedFuture(persistenceResult);
    when(batchProcessor.processBatchAsync(anyList(), anyInt())).thenReturn(future);

    service.run();

    verify(promotionGcpQueryService, times(1))
        .getPromotionTransactionData(any(TransactionPageRequest.class));
    verify(batchProcessor, times(1)).processBatchAsync(transactions, 1);
    verify(metricsClient, times(1)).count(anyString(), anyLong());
    verify(metricsClient, times(1)).recordExecutionTime(anyString(), anyLong(), any());
    verify(metricsClient, times(1)).recordExecutionTime(anyString(), anyLong());
  }

  @Test
  void runWithMultiplePagesSuccess() throws Exception {
    Job mockJob = mock(Job.class);
    TransactionDetailVO transaction1 =
        new TransactionDetailVO(Collections.emptyList(), LocalDate.now(), "txn1");
    TransactionDetailVO transaction2 =
        new TransactionDetailVO(Collections.emptyList(), LocalDate.now(), "txn2");

    List<TransactionDetailVO> page1 = List.of(transaction1);
    List<TransactionDetailVO> page2 = List.of(transaction2);

    TransactionPageResponse response1 =
        new TransactionPageResponse(mockJob, page1, "nextPageToken");
    TransactionPageResponse response2 = new TransactionPageResponse(mockJob, page2, null);

    when(promotionGcpQueryService.getPromotionTransactionData(any(TransactionPageRequest.class)))
        .thenReturn(response1, response2);

    PersistenceResult result1 = new PersistenceResult(1, 1);
    PersistenceResult result2 = new PersistenceResult(2, 1);
    when(batchProcessor.processBatchAsync(anyList(), anyInt()))
        .thenReturn(
            CompletableFuture.completedFuture(result1), CompletableFuture.completedFuture(result2));

    service.run();

    verify(promotionGcpQueryService, times(2))
        .getPromotionTransactionData(any(TransactionPageRequest.class));
    verify(batchProcessor, times(2)).processBatchAsync(anyList(), anyInt());
    verify(metricsClient, times(2)).count(anyString(), anyLong());
  }

  @Test
  void runWithEmptyResultsSuccess() throws Exception {
    Job mockJob = mock(Job.class);
    TransactionPageResponse emptyResponse =
        new TransactionPageResponse(mockJob, Collections.emptyList(), null);

    when(promotionGcpQueryService.getPromotionTransactionData(any(TransactionPageRequest.class)))
        .thenReturn(emptyResponse);

    service.run();

    verify(promotionGcpQueryService, times(1))
        .getPromotionTransactionData(any(TransactionPageRequest.class));
    verify(batchProcessor, never()).processBatchAsync(anyList(), anyInt());
    verify(metricsClient, times(1)).recordExecutionTime(anyString(), anyLong());
  }

  @Test
  void runWithNullResultsSuccess() throws Exception {
    Job mockJob = mock(Job.class);
    TransactionPageResponse nullResponse = new TransactionPageResponse(mockJob, null, null);

    when(promotionGcpQueryService.getPromotionTransactionData(any(TransactionPageRequest.class)))
        .thenReturn(nullResponse);

    service.run();

    verify(promotionGcpQueryService, times(1))
        .getPromotionTransactionData(any(TransactionPageRequest.class));
    verify(batchProcessor, never()).processBatchAsync(anyList(), anyInt());
    verify(metricsClient, times(1)).recordExecutionTime(anyString(), anyLong());
  }

  @Test
  void runWithGcpQueryServiceException() throws Exception {
    when(promotionGcpQueryService.getPromotionTransactionData(any(TransactionPageRequest.class)))
        .thenThrow(new RuntimeException("GCP query failed"));

    assertThrows(RuntimeException.class, () -> service.run());
    verify(metricsClient, times(1)).incrementErrorCount(anyString());
  }

  @Test
  void runWithBatchProcessorFailure() throws Exception {
    Job mockJob = mock(Job.class);
    TransactionDetailVO transaction =
        new TransactionDetailVO(Collections.emptyList(), LocalDate.now(), "txn123");
    List<TransactionDetailVO> transactions = List.of(transaction);

    TransactionPageResponse response = new TransactionPageResponse(mockJob, transactions, null);

    when(promotionGcpQueryService.getPromotionTransactionData(any(TransactionPageRequest.class)))
        .thenReturn(response);

    CompletableFuture<PersistenceResult> failedFuture =
        CompletableFuture.failedFuture(new RuntimeException("Batch processing failed"));
    when(batchProcessor.processBatchAsync(anyList(), anyInt())).thenReturn(failedFuture);

    assertThrows(RuntimeException.class, () -> service.run());

    verify(promotionGcpQueryService, times(1))
        .getPromotionTransactionData(any(TransactionPageRequest.class));
    verify(batchProcessor, times(1)).processBatchAsync(anyList(), anyInt());
    verify(metricsClient, times(1)).incrementErrorCount(anyString());
  }

  @Test
  void runWithPartialBatchFailures() throws Exception {
    Job mockJob = mock(Job.class);
    TransactionDetailVO transaction1 =
        new TransactionDetailVO(Collections.emptyList(), LocalDate.now(), "txn1");
    TransactionDetailVO transaction2 =
        new TransactionDetailVO(Collections.emptyList(), LocalDate.now(), "txn2");

    List<TransactionDetailVO> page1 = List.of(transaction1);
    List<TransactionDetailVO> page2 = List.of(transaction2);

    TransactionPageResponse response1 =
        new TransactionPageResponse(mockJob, page1, "nextPageToken");
    TransactionPageResponse response2 = new TransactionPageResponse(mockJob, page2, null);

    when(promotionGcpQueryService.getPromotionTransactionData(any(TransactionPageRequest.class)))
        .thenReturn(response1, response2);

    PersistenceResult result1 = new PersistenceResult(1, 1);
    CompletableFuture<PersistenceResult> successFuture = CompletableFuture.completedFuture(result1);
    CompletableFuture<PersistenceResult> failedFuture =
        CompletableFuture.failedFuture(new RuntimeException("Batch 2 failed"));

    when(batchProcessor.processBatchAsync(anyList(), anyInt()))
        .thenReturn(successFuture, failedFuture);

    assertThrows(RuntimeException.class, () -> service.run());

    verify(promotionGcpQueryService, times(2))
        .getPromotionTransactionData(any(TransactionPageRequest.class));
    verify(batchProcessor, times(2)).processBatchAsync(anyList(), anyInt());
    verify(metricsClient, times(1)).incrementErrorCount(anyString());
  }
}
