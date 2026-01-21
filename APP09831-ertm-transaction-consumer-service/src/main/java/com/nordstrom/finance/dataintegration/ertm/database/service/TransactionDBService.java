package com.nordstrom.finance.dataintegration.ertm.database.service;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.repository.TransactionRepository;
import com.nordstrom.finance.dataintegration.ertm.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.ertm.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.ertm.metric.Metric;
import com.nordstrom.finance.dataintegration.ertm.metric.MetricTag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDBService {
  private final TransactionRepository transactionRepository;
  private final MetricsClient metricsClient;

  /**
   * * Check for existing transaction IDs in the database.
   *
   * @param transactionId the list of transaction IDs to check
   * @return list of existing transaction IDs
   */
  public List<String> getExistingTransactionIds(List<String> transactionId) {
    log.debug("Checking duplicate for transactionId list of size: {}", transactionId.size());
    List<String> existingIds =
        transactionRepository.getExistingSourceReferenceTransactionIds(transactionId);
    if (existingIds == null || existingIds.isEmpty()) {
      log.info("No duplicate transactions found for the provided transaction IDs.");
    } else {
      log.info("Found {} duplicate transactions.", existingIds.size());
    }
    return existingIds;
  }

  /**
   * * Check for existing line item IDs for a given transaction ID.
   *
   * @param transactionId the transaction ID to check
   * @param lineIds the list of line item IDs to check
   * @return list of existing line item IDs
   */
  public List<String> getExistingLineItemIds(String transactionId, List<String> lineIds) {
    log.debug("Checking duplicate for transactionId={}, lineId={}", transactionId, lineIds);
    List<String> duplicateTransactionIds =
        transactionRepository.getExistingLineItemIds(transactionId, lineIds);
    if (!duplicateTransactionIds.isEmpty()) {
      metricsClient.count(
          Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(), duplicateTransactionIds.size());
    }
    return duplicateTransactionIds;
  }

  /**
   * * Save a single transaction to the database.
   *
   * @param transaction the transaction to save
   * @throws DatabaseConnectionException if there is a database connection issue
   * @throws DatabaseOperationException if there is a database operation issue
   */
  @Transactional
  public void saveTransaction(Transaction transaction)
      throws DatabaseConnectionException, DatabaseOperationException {
    long transactionStartTime = System.currentTimeMillis();
    transactionRepository.save(transaction);
    long transactionDuration = System.currentTimeMillis() - transactionStartTime;
    log.info(
        "Transaction Saved Successfully, Source_reference_transaction_id: {}",
        transaction.getSourceReferenceTransactionId());
    metricsClient.count(Metric.SAVE_TRANSACTION_COUNT.getMetricName(), 1);
    metricsClient.recordExecutionTime(
        Metric.SAVE_TRANSACTION_TIME.getMetricName(), transactionDuration);
  }

  /**
   * * Save a list of transactions to the database in batch.
   *
   * @param transactions the list of transactions to save
   * @throws DatabaseConnectionException if there is a database connection issue
   * @throws DatabaseOperationException if there is a database operation issue
   */
  @Transactional
  public void saveAllTransaction(List<Transaction> transactions)
      throws DatabaseConnectionException, DatabaseOperationException {

    if (transactions.isEmpty()) {
      log.debug("No transactions to save, skipping database operation");
      return;
    }

    long transactionStartTime = System.currentTimeMillis();

    try {
      transactionRepository.saveAll(transactions);
      transactionRepository.flush();
    } catch (Exception e) {
      log.error("Error saving transactions in batch: {}", e.getMessage(), e);
      throw new DatabaseOperationException("Failed to save transactions", e);
    }

    long transactionDuration = System.currentTimeMillis() - transactionStartTime;
    log.info(
        "Transactions Saved Successfully, size: {}, duration: {}ms",
        transactions.size(),
        transactionDuration);

    metricsClient.count(Metric.SAVE_TRANSACTION_COUNT.getMetricName(), transactions.size());
    metricsClient.recordExecutionTime(
        Metric.SAVE_TRANSACTION_TIME.getMetricName(),
        transactionDuration,
        MetricTag.RECORDS_COUNT.getTag(String.valueOf(transactions.size())));
  }

  public List<Transaction> getAll() {
    return transactionRepository.findAll();
  }

  public boolean isReady() {
    try {
      transactionRepository.findById(1L);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
