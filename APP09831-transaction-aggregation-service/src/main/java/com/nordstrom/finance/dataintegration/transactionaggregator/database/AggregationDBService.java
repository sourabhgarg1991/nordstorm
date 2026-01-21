package com.nordstrom.finance.dataintegration.transactionaggregator.database;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.TransactionAggregationRelationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.AggregationConfigurationRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.GeneratedFileDetailRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.TransactionAggregationRelationRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.Metrics;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsErrorCode;
import jakarta.persistence.QueryTimeoutException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service class for handling database operations related to transaction details. It provides
 * methods to save transaction details and check if a transaction has been processed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationDBService {
  private final AggregationConfigurationRepository aggregationConfigurationRepository;
  private final TransactionAggregationRelationRepository transactionAggregationRelationRepository;
  private final GeneratedFileDetailRepository generatedFileDetailRepository;
  private final JdbcTemplate jdbcTemplate;
  private final MetricsClient metricsClient;

  /**
   * Retrieves the current aggregation configurations from the database.
   *
   * @return a list of current aggregation configurations
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  public List<AggregationConfigurationEntity> getCurrentAggregationConfigurations()
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      return aggregationConfigurationRepository.getCurrentConfigurations(LocalDate.now());
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error(
          "Database connection or timeout error while fetching current aggregation configurations from Aurora DB: {}",
          e.getMessage());
      throw DatabaseConnectionException.toAuroraDB();
    } catch (Exception e) {
      log.error(
          "Error getting current aggregation configurations from Aurora DB: {}", e.getMessage());
      throw new DatabaseOperationException(
          "Error getting current aggregation configurations from Aurora DB");
    }
  }

  /**
   * Saves the generated file details into the database.
   *
   * @param generatedFileDetailEntity the generated file detail to save
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  public void saveGeneratedFileDetails(GeneratedFileDetailEntity generatedFileDetailEntity)
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      generatedFileDetailRepository.save(generatedFileDetailEntity);
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error(
          "Database connection error while saving generated file details into Aurora DB: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_CONNECTION_ERROR.name()));
      throw DatabaseConnectionException.toAuroraDB();
    } catch (Exception e) {
      log.error("Error saving generated file details into Aurora DB: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_SAVE_ERROR.name()));
      throw new DatabaseOperationException("Error saving generated file details into Aurora DB.");
    }
  }

  /**
   * * Retrieves the list of generated files that need to be uploaded to S3.
   *
   * @return a list of generated file details to be uploaded to S3
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  public List<GeneratedFileDetailEntity> getGeneratedFileToBeUploadToS3()
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      return generatedFileDetailRepository.getGeneratedFileToBeUploadToS3();
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error(
          "Database connection error while fetching generated file that need to be uploaded to S3 from Aurora DB: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_CONNECTION_ERROR.name()));
      throw DatabaseConnectionException.toAuroraDB();
    } catch (Exception e) {
      log.error(
          "Error while fetching generated file that need to be uploaded to S3 from Aurora DB: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_SAVE_ERROR.name()));
      throw new DatabaseOperationException(
          "Error fetching generated file that need to be uploaded to S3 from Aurora DB.");
    }
  }

  /**
   * * Updates the 'is_uploaded_to_s3' indicator to true for a generated file in the database.
   *
   * @param fileName the name of the generated file to update
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  public void updateGeneratedFileUploadToS3IndicatorToTrue(String fileName)
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      generatedFileDetailRepository.updateGeneratedFileUploadToS3IndicatorToTrue(fileName);
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error(
          "Database connection error while updating the generated file is_uploaded_to_s3 indicator in Aurora DB: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_CONNECTION_ERROR.name()));
      throw DatabaseConnectionException.toAuroraDB();
    } catch (Exception e) {
      log.error(
          "Error while updating the generated file is_uploaded_to_s3 indicator in Aurora DB: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_SAVE_ERROR.name()));
      throw new DatabaseOperationException(
          "Error updating the generated file is_uploaded_to_s3 indicator in Aurora DB.");
    }
  }

  /**
   * Saves the aggregation details into the database.
   *
   * @param generatedFileDetails List of aggregation detail to save
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  public void saveTransactionAggregationRelationshipDetails(
      List<TransactionAggregationRelationEntity> generatedFileDetails)
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      transactionAggregationRelationRepository.saveAll(generatedFileDetails);
    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error(
          "Database connection error while saving aggregation details into Aurora DB: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_CONNECTION_ERROR.name()));
      throw DatabaseConnectionException.toAuroraDB();
    } catch (Exception e) {
      log.error("Error saving aggregation details into Aurora DB: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_SAVE_ERROR.name()));
      throw new DatabaseOperationException("Error saving aggregation details into Aurora DB.");
    }
  }

  /**
   * Executes a SQL query and generates a CSV representation of the result set.
   *
   * @param query the SQL query to execute
   * @return a list of string arrays representing the rows of the result set, with the first row as
   *     the header
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  public List<String[]> executeAggregationQuery(String query)
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      long queryStartTime = System.currentTimeMillis();
      List<String[]> rows =
          jdbcTemplate.query(
              query,
              (ResultSet rs) -> {
                List<String[]> results = new ArrayList<>();

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                String[] headers = new String[columnCount];

                // Add data rows
                while (rs.next()) {
                  if (results.isEmpty()) {

                    for (int i = 1; i <= columnCount; i++) {
                      headers[i - 1] = metaData.getColumnName(i);
                    }
                    results.add(headers);
                  }
                  String[] row = new String[columnCount];
                  for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getString(i);
                  }
                  results.add(row);
                }
                return results;
              });
      log.info(
          "Aggregation Query executed successfully. Number of rows returned: {}", rows.size() - 1);
      metricsClient.recordExecutionTime(
          Metrics.QUERY_EXECUTION_TIME.getMetricName(),
          System.currentTimeMillis() - queryStartTime);
      return rows;

    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error(
          "Unexpected Database connection error while executing aggregation details query: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_CONNECTION_ERROR.name()));
      throw DatabaseConnectionException.toAuroraDB();
    } catch (Exception e) {
      log.error("Unexpected Error while executing aggregation details query: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.QUERY_EXECUTION_ERROR.name()));
      throw new DatabaseOperationException("Error while executing aggregation details query.");
    }
  }

  /**
   * Executes a control data SQL query and retrieves a single string result. Requirement: The
   * control data SQL query returns the total aggregated amount for a given aggregation
   * configuration.
   *
   * @param query the SQL query to execute
   * @return the string result of the query
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  public String executeControlDataQuery(String query)
      throws DatabaseConnectionException, DatabaseOperationException {
    try {
      long queryStartTime = System.currentTimeMillis();
      String totalAmount =
          jdbcTemplate.query(
              query,
              (ResultSet rs) -> {
                // Add data rows
                if (rs.next()) {
                  return rs.getString(1);
                }
                return "0";
              });
      log.info(
          "Aggregation Control data query executed successfully. Total amount: {}", totalAmount);
      metricsClient.recordExecutionTime(
          Metrics.CONTROL_QUERY_EXECUTION_TIME.getMetricName(),
          System.currentTimeMillis() - queryStartTime);
      return totalAmount;

    } catch (JDBCConnectionException | QueryTimeoutException e) {
      log.error(
          "Unexpected Database connection error while executing aggregation details control query: {}",
          e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.DB_CONNECTION_ERROR.name()));
      throw DatabaseConnectionException.toAuroraDB();
    } catch (Exception e) {
      log.error("Unexpected Error executing aggregation details control query {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.QUERY_EXECUTION_ERROR.name()));
      throw new DatabaseOperationException("Error executing aggregation details control query.");
    }
  }
}
