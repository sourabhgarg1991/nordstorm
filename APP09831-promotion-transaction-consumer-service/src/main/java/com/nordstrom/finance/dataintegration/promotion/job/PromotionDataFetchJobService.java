package com.nordstrom.finance.dataintegration.promotion.job;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageRequest;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageResponse;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.service.PromotionGcpQueryService;
import com.nordstrom.finance.dataintegration.promotion.domain.model.PersistenceResult;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import com.nordstrom.finance.dataintegration.promotion.metric.Metrics;
import com.nordstrom.finance.dataintegration.promotion.metric.MetricsErrorCode;
import com.nordstrom.finance.dataintegration.promotion.metric.MetricsTag;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * App-level entry point for scheduled data fetch jobs (K8s CronJob, etc.). Fetches all pages of
 * promotion transaction data from GCP and persists to Aurora DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionDataFetchJobService implements CommandLineRunner {
  private final PromotionGcpQueryService promotionGcpQueryService;
  private final PromotionBatchProcessor batchProcessor;
  private final MetricsClient metricsClient;

  @Override
  public void run(String... args) {
    log.info("Promotion Data Fetch Job triggered at {}", LocalDateTime.now());
    long startTime = System.currentTimeMillis();
    int pageCount = 1;
    long totalFetched = 0;
    long totalPersisted = 0;
    List<CompletableFuture<PersistenceResult>> futures = new ArrayList<>();

    try {
      TransactionPageRequest request = TransactionPageRequest.firstPage();
      TransactionPageResponse response;

      do {
        log.info("Fetching promotion transactions from GCP BigQuery, page #{}", pageCount);
        long fetchStartTime = System.currentTimeMillis();

        response = promotionGcpQueryService.getPromotionTransactionData(request);
        List<TransactionDetailVO> currentPage = response.currentPageTransactions();

        if (currentPage == null || currentPage.isEmpty()) {
          log.info("No records found on page #{}", pageCount);
          break;
        }

        log.info(
            "Fetched {} records from GCP BigQuery on page #{} in {} ms",
            currentPage.size(),
            pageCount,
            System.currentTimeMillis() - fetchStartTime);

        totalFetched += currentPage.size();

        CompletableFuture<PersistenceResult> future =
            batchProcessor.processBatchAsync(currentPage, pageCount);
        futures.add(future);

        request = TransactionPageRequest.nextPage(response);
        pageCount++;

        metricsClient.count(Metrics.GCP_FETCH_COUNT.getMetricName(), currentPage.size());
        metricsClient.recordExecutionTime(
            Metrics.GCP_PAGE_FETCH_TIME.getMetricName(),
            System.currentTimeMillis() - fetchStartTime,
            MetricsTag.PAGE_NUMBER.getTag(String.valueOf(pageCount)));
      } while (response.hasNext());

      // Wait for all async processing to complete
      log.info("Waiting for {} page(s) to be processed...", futures.size());
      CompletableFuture<Void> allOf =
          CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
      allOf.join();

      for (CompletableFuture<PersistenceResult> future : futures) {
        try {
          PersistenceResult result = future.join();
          totalPersisted += result.persisted();
        } catch (CompletionException e) {
          log.error("Error processing batch: {}", e.getCause().getMessage());
        }
      }

      long executionTime = System.currentTimeMillis() - startTime;
      metricsClient.recordExecutionTime(Metrics.JOB_EXECUTION_TIME.getMetricName(), executionTime);
      log.info(
          "=========== Job Completion Summary ===========\nTotal records fetched from GCP: {}\nTotal new records persisted: {}\nTotal execution time: {} seconds\n",
          totalFetched,
          totalPersisted,
          executionTime / 1000);

      // force exit after short delay to ensure all logs/metrics are flushed
      log.info("Cronjob completed. Application exit.");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // Ignore
      }
      System.exit(0);
    } catch (Exception e) {
      log.error(
          "Critical error during Promotion Data Fetch Job on page #{}: {}",
          pageCount,
          e.getMessage(),
          e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.JOB_EXECUTION_ERROR.name()));
      throw new RuntimeException("Job failed", e);
    }
  }
}
