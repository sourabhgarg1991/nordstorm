package com.nordstrom.finance.dataintegration.promotion.job;

import static com.nordstrom.finance.dataintegration.promotion.database.gcp.constant.GcpQueryConstants.GCP_EXECUTOR_BEAN_NAME;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.service.PromotionConsumerDBService;
import com.nordstrom.finance.dataintegration.promotion.domain.model.PersistenceResult;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import com.nordstrom.finance.dataintegration.promotion.metric.Metrics;
import com.nordstrom.finance.dataintegration.promotion.metric.MetricsErrorCode;
import com.nordstrom.finance.dataintegration.promotion.metric.MetricsTag;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionBatchProcessor {

  private final PromotionConsumerDBService promotionConsumerDBService;
  private final MetricsClient metricsClient;

  @Async(GCP_EXECUTOR_BEAN_NAME)
  public CompletableFuture<PersistenceResult> processBatchAsync(
      List<TransactionDetailVO> transactions, int pageNumber) {

    long startTime = System.currentTimeMillis();
    log.info(
        "Starting async processing of page #{} with {} transactions",
        pageNumber,
        transactions.size());

    try {
      // duplicate checking and persistence
      int persistedSize = promotionConsumerDBService.processAndPersistPromotionBatch(transactions);

      log.info(
          "Page #{} processed successfully in {} ms",
          pageNumber,
          System.currentTimeMillis() - startTime);

      metricsClient.recordExecutionTime(
          Metrics.BATCH_PROCESSING_TIME.getMetricName(),
          System.currentTimeMillis() - startTime,
          MetricsTag.PAGE_NUMBER.getTag(String.valueOf(pageNumber)),
          MetricsTag.BATCH_SIZE.getTag(String.valueOf(transactions.size())));
      return CompletableFuture.completedFuture(new PersistenceResult(pageNumber, persistedSize));

    } catch (Exception e) {
      log.error("Failed to process page #{}: {}", pageNumber, e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.BATCH_PROCESSING_ERROR.name()));
      return CompletableFuture.failedFuture(e);
    }
  }
}
