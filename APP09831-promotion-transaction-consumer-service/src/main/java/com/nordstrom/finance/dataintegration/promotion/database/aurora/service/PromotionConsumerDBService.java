package com.nordstrom.finance.dataintegration.promotion.database.aurora.service;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.Transaction;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.repository.TransactionRepository;
import com.nordstrom.finance.dataintegration.promotion.domain.mapper.PromotionEntityMapper;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import com.nordstrom.finance.dataintegration.promotion.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.promotion.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.promotion.metric.Metrics;
import com.nordstrom.finance.dataintegration.promotion.metric.MetricsErrorCode;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionConsumerDBService {

  private final TransactionRepository transactionRepository;
  private final MetricsClient metricsClient;

  /**
   * Processes and persists promotion transactions from GCP batch data.
   *
   * <p>This method performs the following operations:
   *
   * <ol>
   *   <li>Extracts global transaction IDs from the input batch
   *   <li>Queries the database to identify existing transactions
   *   <li>Filters out duplicates based on global transaction ID
   *   <li>Maps remaining transactions to JPA entities
   *   <li>Persists new transactions with cascading to child entities
   * </ol>
   *
   * The method ensures idempotency by skipping transactions that already exist in the database,
   * preventing duplicate records when processing the same GCP page multiple times.
   *
   * @param transactionDetailVOList batch of transaction details fetched from GCP BigQuery,
   *     typically representing one page of data (e.g., 2000 records)
   * @throws DatabaseConnectionException if unable to establish or maintain database connection
   * @throws DatabaseOperationException if the persistence operation fails after connection is
   *     established
   */
  @Transactional
  public int processAndPersistPromotionBatch(List<TransactionDetailVO> transactionDetailVOList)
      throws DatabaseConnectionException, DatabaseOperationException {

    if (CollectionUtils.isEmpty(transactionDetailVOList)) {
      log.debug("No transactions to process - empty list provided");
      return 0;
    }

    try {
      // Step 1: Extract all global transaction IDs from the input list
      List<String> globalTransactionIds = extractGlobalTransactionIds(transactionDetailVOList);
      log.info("Processing {} transactions from GCP page", globalTransactionIds.size());

      // Step 2: Find which transaction IDs already exist in the database
      Set<String> existingTransactionIds =
          transactionRepository.findExistingSourceReferenceTransactionIds(globalTransactionIds);

      if (!existingTransactionIds.isEmpty()) {
        log.info(
            "Found {} duplicate transactions that already exist in database",
            existingTransactionIds.size());
        metricsClient.count(
            Metrics.DUPLICATE_TRANSACTION_COUNT.getMetricName(), existingTransactionIds.size());
      }

      // Step 3: Filter out transactions that already exist
      List<TransactionDetailVO> newTransactions =
          filterNewTransactions(transactionDetailVOList, existingTransactionIds);

      if (newTransactions.isEmpty()) {
        log.info(
            "All {} transactions already exist in database - skipping persistence",
            transactionDetailVOList.size());
        return 0;
      }

      log.info("Processing {} new transactions for persistence", newTransactions.size());

      // Step 4: Map VOs to entities
      List<Transaction> transactionEntities =
          PromotionEntityMapper.mapToTransactions(newTransactions);

      // Step 5: Persist all new transactions (cascades will save all children)
      long persistStartTime = System.currentTimeMillis();
      List<Transaction> savedTransactions = transactionRepository.saveAll(transactionEntities);
      long persistTime = System.currentTimeMillis() - persistStartTime;

      int totalLines =
          savedTransactions.stream().mapToInt(t -> t.getTransactionLines().size()).sum();
      int totalPromotions =
          savedTransactions.stream()
              .flatMap(t -> t.getTransactionLines().stream())
              .mapToInt(tl -> tl.getPromotionTransactionLines().size())
              .sum();

      log.info(
          "Successfully persisted {} transactions with {} transaction lines and {} promotion lines",
          savedTransactions.size(),
          totalLines,
          totalPromotions);

      metricsClient.count(
          Metrics.PERSISTED_TRANSACTION_COUNT.getMetricName(), savedTransactions.size());
      metricsClient.recordExecutionTime(Metrics.PERSISTENCE_TIME.getMetricName(), persistTime);
      return savedTransactions.size();
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error("Database connection error while persisting promotion lines: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_CONNECTION_ERROR.name()));
      throw new DatabaseConnectionException("Failed to connect to Aurora DB", e);
    } catch (Exception e) {
      log.error("Failed to persist promotion lines for batch processing", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.PERSISTENCE_ERROR.name()));
      throw new DatabaseOperationException("Error saving promotion lines", e);
    }
  }

  /**
   * Extracts global transaction IDs from the list of transaction detail VOs.
   *
   * @param transactionDetailVOList list of transaction details
   * @return list of global transaction IDs
   */
  private List<String> extractGlobalTransactionIds(
      List<TransactionDetailVO> transactionDetailVOList) {
    return transactionDetailVOList.stream()
        .map(TransactionDetailVO::globalTransactionId)
        .filter(id -> id != null && !id.trim().isEmpty())
        .distinct()
        .toList();
  }

  /**
   * Filters out transactions that already exist in the database.
   *
   * @param transactionDetailVOList original list of transactions
   * @param existingTransactionIds set of transaction IDs that already exist
   * @return list of new transactions that don't exist in database
   */
  private List<TransactionDetailVO> filterNewTransactions(
      List<TransactionDetailVO> transactionDetailVOList, Set<String> existingTransactionIds) {

    if (existingTransactionIds.isEmpty()) return transactionDetailVOList;

    return transactionDetailVOList.stream()
        .filter(vo -> !existingTransactionIds.contains(vo.globalTransactionId()))
        .toList();
  }
}
