package com.nordstrom.finance.dataintegration.database.service;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.database.entity.FilteredTransaction;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.repository.FilteredTransactionRepository;
import com.nordstrom.finance.dataintegration.database.repository.TransactionRepository;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.metric.Metric;
import com.nordstrom.finance.dataintegration.metric.MetricErrorCode;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDBService {
  private final TransactionRepository transactionRepository;
  private final FilteredTransactionRepository filteredTransactionRepository;
  private final MetricsClient metricsClient;

  /**
   * Persist a Transaction and its associated lines atomically.
   *
   * <p>This method saves the Transaction entity and, via JPA cascading, all related
   * TransactionLine, RestaurantTransactionLine, and MarketplaceTransactionLine entities. It ensures
   * that either all entities are saved or none are, maintaining data consistency. Duplicate
   * detection should be handled before calling this method to avoid partial saves.
   *
   * @param transaction the Transaction entity to persist
   * @throws DatabaseConnectionException if a database connection or query timeout error occurs
   * @throws DatabaseOperationException for all other database operation errors
   */
  @Transactional
  public void saveTransaction(Transaction transaction)
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      long transactionStartTime = System.currentTimeMillis();
      transactionRepository.save(transaction);
      metricsClient.incrementCounter(Metric.SAVE_TRANSACTION_COUNT.getMetricName());
      metricsClient.recordExecutionTime(
          Metric.SAVE_TRANSACTION_TIME.getMetricName(),
          System.currentTimeMillis() - transactionStartTime);
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error("DB connection error: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SAVE_TRANSACTIONS_ERROR_COUNT.getErrorValue())));
      throw new DatabaseConnectionException("Failed to connect to DB");
    } catch (Exception e) {
      log.error("Error saving Transaction.", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SAVE_TRANSACTIONS_ERROR_COUNT.getErrorValue())));
      throw new DatabaseOperationException("Error saving transaction", e);
    }
  }

  /**
   * Retrieve a Transaction by its primary key.
   *
   * @param id the Transaction primary key
   * @return an Optional containing the Transaction if found, or empty if not
   */
  public Optional<Transaction> findById(Long id) {
    return transactionRepository.findById(id);
  }

  /**
   * Delete a Transaction by its primary key.
   *
   * @param id the Transaction primary key
   */
  public void deleteById(Long id) {
    transactionRepository.deleteById(id);
  }

  /**
   * Check if a Transaction exists for the given identifiers.
   *
   * @param systemType the source system type
   * @param sourceReferenceType the business line
   * @param transactionId the transaction identifier
   * @return true if the Transaction exists, false otherwise
   */
  public boolean existsByTransactionId(
      String systemType, String sourceReferenceType, String transactionId) {
    try {
      return transactionRepository.existsByTransactionId(
          systemType, sourceReferenceType, transactionId);
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error("DB connection error: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.FIND_DUPLICATE_SDM_ID_DB_ERROR.getErrorValue())));
      throw new DatabaseConnectionException("Failed to connect to DB");
    } catch (Exception e) {
      log.error("Error while fetching the Transaction.", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.FIND_DUPLICATE_SDM_ID_DB_ERROR.getErrorValue())));
      throw new DatabaseOperationException("Error saving filtered transaction", e);
    }
  }

  @Transactional
  public void saveFilteredTransaction(FilteredTransaction filteredTransaction)
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      long transactionStartTime = System.currentTimeMillis();
      filteredTransactionRepository.save(filteredTransaction);
      metricsClient.recordExecutionTime(
          Metric.SAVE_TRANSACTION_TIME.getMetricName(),
          System.currentTimeMillis() - transactionStartTime);
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error("DB connection error: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SAVE_TRANSACTIONS_ERROR_COUNT.getErrorValue())));
      throw new DatabaseConnectionException("Failed to connect to DB");
    } catch (Exception e) {
      log.error("Error saving filtered Transaction.", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SAVE_TRANSACTIONS_ERROR_COUNT.getErrorValue())));
      throw new DatabaseOperationException("Error saving filtered transaction", e);
    }
  }

  /**
   * Retrieve all Transactions.
   *
   * @return a list of all Transactions
   */
  public List<Transaction> retrieveAllTransaction() {
    try {
      return transactionRepository.findAll();
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error("DB connection error: {}", e.getMessage());
      throw new DatabaseConnectionException("Failed to connect to DB");
    } catch (Exception e) {
      log.error("Error while fetching all Transactions.", e);
      throw new DatabaseOperationException("Error fetching all transactions", e);
    }
  }

  public boolean isReady() {
    try {
      filteredTransactionRepository.findById(1L);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
