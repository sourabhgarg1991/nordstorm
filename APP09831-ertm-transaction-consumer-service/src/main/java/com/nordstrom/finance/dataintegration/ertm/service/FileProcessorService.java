package com.nordstrom.finance.dataintegration.ertm.service;

import com.nordstrom.finance.dataintegration.common.aws.S3Utility;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.ertm.config.AwsServiceConfig;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.RetailTransactionLine;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.ertm.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.ertm.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.ertm.exception.FileMappingException;
import com.nordstrom.finance.dataintegration.ertm.mapper.RetailTransactionLineMapper;
import com.nordstrom.finance.dataintegration.ertm.metric.Metric;
import com.nordstrom.finance.dataintegration.ertm.metric.MetricErrorCode;
import com.nordstrom.finance.dataintegration.ertm.metric.MetricTag;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service responsible for processing CSV files from S3 and storing transaction data in the
 * database. Handles large file processing (1GB+) using streaming batch approach with configurable
 * batch sizes.
 *
 * <p>Main functionality: - Downloads CSV files from S3 source bucket - Processes files in
 * configurable batches (default: 2000 records) - Manages transaction boundaries across batch
 * processing - Filters duplicate transactions using database lookup - Maps CSV data to database
 * entities and persists them - Moves processed files to archive bucket - Provides comprehensive
 * metrics and error handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessorService {

  // === DEPENDENCIES ===
  private final AwsServiceConfig awsServiceConfig;
  private final TransactionDBService transactionDBService;
  private final RetailTransactionLineMapper retailTransactionLineMapper;
  private final MetricsClient metricsClient;
  private final S3Utility s3Utility;

  // === CONFIGURATION ===
  @Value("${file.processing.batch.size:30000}")
  private int batchSize;

  private static final int BUFFER_SIZE_KB = 64 * 1024; // 64KB buffer for large files

  /**
   * Main entry point: Process all CSV files from S3 bucket and store data in database. Downloads
   * files from source bucket, processes them in batches, and moves to processed bucket.
   */
  public void processCsvFromS3() {
    final String sourceBucket = awsServiceConfig.getSourceBucket();
    final String processedBucket = awsServiceConfig.getProcessedBucket();
    final long overallStartTime = System.currentTimeMillis();

    log.info(
        "Starting CSV processing from S3 bucket: {} with batch size: {}", sourceBucket, batchSize);
    try {
      final List<String> fileList = s3Utility.listFileKeys(sourceBucket);

      if (fileList.isEmpty()) {
        log.info("No files found in source bucket: {}", sourceBucket);
        return;
      }

      log.info("Found {} files to process", fileList.size());

      // Process each file individually with error isolation
      for (String fileName : fileList) {
        processIndividualFile(fileName, sourceBucket, processedBucket);
      }

    } catch (Exception ex) {
      log.error("Error connecting to S3 bucket {}: {}", sourceBucket, ex.getMessage(), ex);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricErrorCode.AWS_S3_CONNECTION_ERROR.name()));
    } finally {
      // Record total processing time
      recordBucketProcessingMetrics(overallStartTime);
    }

    log.info("Successfully completed processing all files from S3 bucket: {}", sourceBucket);
  }

  /**
   * Process file data from a BufferedReader using streaming batch approach. Handles transaction
   * boundary management and prevents OOM for large files.
   *
   * @param reader BufferedReader containing CSV data
   * @throws DatabaseConnectionException if database connection fails
   * @throws DatabaseOperationException if database operations fail
   * @throws FileMappingException if CSV parsing or mapping fails
   */
  public void processFileData(BufferedReader reader)
      throws DatabaseConnectionException, DatabaseOperationException, FileMappingException {

    log.info("Processing file data in streaming batches of {} records", batchSize);

    try {
      processFileDataInStreamingBatches(reader);
    } catch (Exception ex) {
      log.error("Critical error during file processing: {}", ex.getMessage(), ex);
      throw new FileMappingException("Error reading file data and mapping to objects");
    }
    log.info("File data processing completed successfully");
  }

  /** Process an individual file with comprehensive error handling and metrics. */
  private void processIndividualFile(String fileName, String sourceBucket, String processedBucket) {
    boolean isProcessingSuccessful = false;
    log.info("Processing file: {} from bucket: {}", fileName, sourceBucket);
    final long fileStartTime = System.currentTimeMillis();

    try (BufferedReader reader = createOptimizedFileReader(fileName, sourceBucket)) {

      log.info("Successfully retrieved file {} from S3 bucket {}", fileName, sourceBucket);
      processFileData(reader);
      moveFileToProcessedBucket(fileName, sourceBucket, processedBucket);
      isProcessingSuccessful = true;
    } catch (IOException e) {
      handleFileReadError(e, fileName);
    } catch (DatabaseConnectionException | DatabaseOperationException ex) {
      handleDatabaseError(ex, fileName);
    } catch (FileMappingException ex) {
      handleMappingError(ex, fileName);
    } finally {
      if (isProcessingSuccessful) {
        log.info("Successfully completed processing file '{}'.", fileName);
      } else {
        log.warn("Completed processing file '{}' with error.", fileName);
      }
      recordFileProcessingMetrics(fileName, fileStartTime);
    }

    log.info("Successfully completed processing file: {}", fileName);
  }

  /** Creates an optimized BufferedReader for S3 file processing. */
  private BufferedReader createOptimizedFileReader(String fileName, String sourceBucket)
      throws IOException {
    return new BufferedReader(
        new InputStreamReader(s3Utility.downloadFileAsStream(fileName, sourceBucket)),
        BUFFER_SIZE_KB);
  }

  /** Moves processed file to archive bucket with error handling. */
  private void moveFileToProcessedBucket(
      String fileName, String sourceBucket, String processedBucket) {
    final boolean moveSuccess =
        s3Utility.moveFileToAnotherBucket(fileName, sourceBucket, processedBucket);
    if (moveSuccess) {
      log.info("Successfully moved file {} to processed bucket", fileName);
    } else {
      log.warn(
          "Failed to move file {} to processed bucket - file may still exist in source", fileName);
    }
  }

  /** Process file data in streaming batches to handle large files efficiently. */
  private void processFileDataInStreamingBatches(BufferedReader reader)
      throws DatabaseConnectionException, DatabaseOperationException, FileMappingException {

    int batchNumber = 1;
    int totalRecordsProcessed = 0;
    List<RetailTransactionLineDTO> overflowFromPreviousBatch = new ArrayList<>(batchSize);
    List<RetailTransactionLineDTO> currentBatch = new ArrayList<>(batchSize);

    try {
      final Iterator<RetailTransactionLineDTO> csvIterator = createStreamingCsvIterator(reader);

      log.info("Starting streaming CSV processing in batches of {} records", batchSize);
      // Add any overflow records from previous batch
      currentBatch.addAll(overflowFromPreviousBatch);
      overflowFromPreviousBatch.clear();

      int totalRecordsInFile = 0;

      // Stream through CSV records without loading entire file into memory
      while (csvIterator.hasNext()) {
        totalRecordsInFile++;

        try {
          final RetailTransactionLineDTO record = csvIterator.next();
          currentBatch.add(record);

          // Process batch when we reach configured batch size
          if (currentBatch.size() >= batchSize) {
            final ProcessBatchResult result =
                processBatchWithTransactionBoundaries(currentBatch, batchNumber);

            // Update counters and prepare next batch
            totalRecordsProcessed += result.recordsProcessed;
            currentBatch = new ArrayList<>(batchSize);
            currentBatch.addAll(result.overflow);
            batchNumber++;
          }

        } catch (Exception ex) {
          log.error(
              "Error processing record {} in batch {}: {}",
              totalRecordsInFile,
              batchNumber,
              ex.getMessage(),
              ex);
          throw new FileMappingException("Error processing CSV record in streaming mode");
        }
      }

      // Process any remaining records in the final batch
      if (!currentBatch.isEmpty()) {
        log.info("Processing final batch {} with {} records", batchNumber, currentBatch.size());
        final ProcessBatchResult finalResult =
            processBatchWithTransactionBoundaries(currentBatch, batchNumber);
        totalRecordsProcessed += finalResult.recordsProcessed;
      }

      log.info(
          "Streaming processing completed successfully. Total records processed: {}",
          totalRecordsProcessed);

    } catch (Exception ex) {
      log.error("Error in streaming batch processing: {}", ex.getMessage(), ex);
      throw ex;
    }
  }

  /** Creates a streaming CSV iterator for memory-efficient processing. */
  private Iterator<RetailTransactionLineDTO> createStreamingCsvIterator(BufferedReader reader) {
    return new CsvToBeanBuilder<RetailTransactionLineDTO>(reader)
        .withIgnoreLeadingWhiteSpace(true)
        .withType(RetailTransactionLineDTO.class)
        .withSeparator('|')
        .build()
        .iterator();
  }

  /** Process a single batch with transaction boundary management and database persistence. */
  private ProcessBatchResult processBatchWithTransactionBoundaries(
      List<RetailTransactionLineDTO> batchRecords, int batchNumber)
      throws DatabaseConnectionException, DatabaseOperationException {

    final long batchStartTime = System.currentTimeMillis();

    // Separate complete transactions from overflow (transaction boundary management)
    final ProcessBatchResult result =
        determineCompleteTransactionsAndOverflow(batchRecords, batchNumber);

    // Save processed transactions to database
    saveTransactionsWithMetrics(result, batchNumber);

    // Record comprehensive batch processing metrics
    recordBatchProcessingMetrics(batchNumber, result, batchStartTime);

    return result;
  }

  /**
   * Determine which transactions are complete vs need to overflow to next batch. This ensures
   * transaction integrity across batch boundaries.
   */
  private ProcessBatchResult determineCompleteTransactionsAndOverflow(
      List<RetailTransactionLineDTO> batchRecords, int batchNumber) {

    if (batchRecords.isEmpty()) {
      return new ProcessBatchResult(new ArrayList<>(), new ArrayList<>(), 0);
    }

    log.debug("Processing batch {} with {} records", batchNumber, batchRecords.size());

    // Group records by transaction ID for boundary analysis
    final Map<String, List<RetailTransactionLineDTO>> groupedByTransactionId =
        groupRecordsByTransactionId(batchRecords);

    // Simplified approach: if batch is full and has multiple transactions,
    // move the last transaction to overflow to ensure completeness
    final boolean isFullBatch = batchRecords.size() >= batchSize;

    List<RetailTransactionLineDTO> completeTransactionRecords = new ArrayList<>();
    List<RetailTransactionLineDTO> overflowRecords = new ArrayList<>();

    if (isFullBatch && groupedByTransactionId.size() > 1) {
      // Find the transaction ID that appears last in the batch
      String lastTransactionId =
          batchRecords.get(batchRecords.size() - 1).getSourceReferenceTransactionId();

      // Move the last transaction to overflow, process all others as complete
      for (Map.Entry<String, List<RetailTransactionLineDTO>> entry :
          groupedByTransactionId.entrySet()) {
        String txnId = entry.getKey();
        List<RetailTransactionLineDTO> txnRecords = entry.getValue();

        if (txnId.equals(lastTransactionId)) {
          overflowRecords.addAll(txnRecords);
        } else {
          completeTransactionRecords.addAll(txnRecords);
        }
      }
    } else {
      // Not a full batch, or only one transaction - process all as complete
      groupedByTransactionId.values().forEach(completeTransactionRecords::addAll);
    }

    // Convert complete transactions to database entities
    final List<Transaction> processedTransactions =
        convertCompleteTransactionsToEntities(completeTransactionRecords);

    log.debug(
        "Batch {}: {} complete transactions, {} overflow records",
        batchNumber,
        processedTransactions.size(),
        overflowRecords.size());

    return new ProcessBatchResult(
        processedTransactions, overflowRecords, completeTransactionRecords.size());
  }

  /** Groups CSV records by transaction ID for processing. */
  private Map<String, List<RetailTransactionLineDTO>> groupRecordsByTransactionId(
      List<RetailTransactionLineDTO> batchRecords) {
    return batchRecords.stream()
        .collect(Collectors.groupingBy(RetailTransactionLineDTO::getSourceReferenceTransactionId));
  }

  /** Convert complete transaction records to database entities. */
  private List<Transaction> convertCompleteTransactionsToEntities(
      List<RetailTransactionLineDTO> completeTransactionRecords) {

    if (completeTransactionRecords.isEmpty()) {
      return new ArrayList<>();
    }

    final Map<String, List<RetailTransactionLineDTO>> completeGrouped =
        groupRecordsByTransactionId(completeTransactionRecords);

    return filterDuplicateTransactions(completeGrouped).stream()
        .map(this::mapTransactionLinesToEntity)
        .filter(Objects::nonNull)
        .toList();
  }

  /** Save processed transactions with comprehensive metrics and logging. */
  private void saveTransactionsWithMetrics(ProcessBatchResult result, int batchNumber)
      throws DatabaseConnectionException, DatabaseOperationException {

    if (!result.transactionsToSave.isEmpty()) {
      transactionDBService.saveAllTransaction(result.transactionsToSave);
      log.info(
          "Batch {} completed: {} transactions saved, {} records processed, {} overflow records",
          batchNumber,
          result.transactionsToSave.size(),
          result.recordsProcessed,
          result.overflow.size());
    } else {
      log.info(
          "Batch {} completed: No new transactions to save, {} records processed, {} overflow records",
          batchNumber,
          result.recordsProcessed,
          result.overflow.size());
    }
  }

  // === DATA TRANSFORMATION METHODS ===

  /** Filter out duplicate transactions based on database lookup. */
  private List<List<RetailTransactionLineDTO>> filterDuplicateTransactions(
      Map<String, List<RetailTransactionLineDTO>> groupedByTransactionId) {

    List<String> existingTransactionIds =
        transactionDBService.getExistingTransactionIds(
            new ArrayList<>(groupedByTransactionId.keySet()));
    if (existingTransactionIds == null || existingTransactionIds.isEmpty()) {
      return new ArrayList<>(groupedByTransactionId.values());
    }

    return groupedByTransactionId.entrySet().stream()
        .map(
            entry -> {
              final String transactionId = entry.getKey();
              if (existingTransactionIds.contains(transactionId)) {
                final List<RetailTransactionLineDTO> transactionLines = entry.getValue();
                final List<String> lineIds = extractLineIds(transactionLines);
                final List<String> duplicateLineIds =
                    transactionDBService.getExistingLineItemIds(transactionId, lineIds);
                return removeDuplicateLines(duplicateLineIds, transactionLines);
              } else {
                return entry.getValue();
              }
            })
        .toList();
  }

  /** Extract line IDs from transaction lines for duplicate checking. */
  private List<String> extractLineIds(List<RetailTransactionLineDTO> transactionLines) {
    return transactionLines.stream()
        .map(RetailTransactionLineDTO::getSourceReferenceLineId)
        .toList();
  }

  /** Remove duplicate transaction lines based on duplicate line IDs. */
  private List<RetailTransactionLineDTO> removeDuplicateLines(
      List<String> duplicateLineIds, List<RetailTransactionLineDTO> transactionLines) {

    if (duplicateLineIds == null || duplicateLineIds.isEmpty()) {
      return transactionLines;
    }

    return transactionLines.stream()
        .filter(line -> !duplicateLineIds.contains(line.getSourceReferenceLineId()))
        .toList();
  }

  /** Maps a list of RetailTransactionLineDTO to a single Transaction entity. */
  private Transaction mapTransactionLinesToEntity(List<RetailTransactionLineDTO> transactionLines) {
    Transaction transaction = null;

    for (RetailTransactionLineDTO retailTransactionDTO : transactionLines) {
      final RetailTransactionLine retailTransactionLine =
          retailTransactionLineMapper.mapRecordToRetailTransactionLine(
              retailTransactionDTO, transaction);
      transaction = retailTransactionLine.getTransactionLine().getTransaction();
    }

    return transaction;
  }

  // === ERROR HANDLING METHODS ===

  /** Handle file read errors with specific metrics. */
  private void handleFileReadError(IOException e, String fileName) {
    log.error("Error reading CSV file {} from S3: {}", fileName, e.getMessage(), e);
    metricsClient.incrementErrorCount(
        MetricsCommonTag.ERROR_CODE.getTag(MetricErrorCode.AWS_S3_FILE_READ_ERROR.name()),
        MetricTag.FILE_NAME.getTag(fileName));
  }

  /** Handle database errors with specific metrics. */
  private void handleDatabaseError(RuntimeException ex, String fileName) {
    log.error("Error saving file {} data to database: {}", fileName, ex.getMessage(), ex);
    metricsClient.incrementErrorCount(
        MetricsCommonTag.ERROR_CODE.getTag(MetricErrorCode.DB_CONNECTION_ERROR.name()),
        MetricTag.FILE_NAME.getTag(fileName));
  }

  /** Handle mapping errors with specific metrics. */
  private void handleMappingError(FileMappingException ex, String fileName) {
    log.error("Error mapping file {} data to objects: {}", fileName, ex.getMessage(), ex);
    metricsClient.incrementErrorCount(
        MetricsCommonTag.ERROR_CODE.getTag(MetricErrorCode.ENTITY_MAPPING_ERROR.name()),
        MetricTag.FILE_NAME.getTag(fileName));
  }

  // === METRICS RECORDING METHODS ===

  /** Record comprehensive file processing metrics. */
  private void recordFileProcessingMetrics(String fileName, long startTime) {
    final long duration = System.currentTimeMillis() - startTime;
    metricsClient.recordExecutionTime(
        Metric.FILE_PROCESSING_TIME.getMetricName(),
        duration,
        MetricTag.FILE_NAME.getTag(fileName));
  }

  /** Record bucket processing metrics. */
  private void recordBucketProcessingMetrics(long startTime) {
    final long totalDuration = System.currentTimeMillis() - startTime;
    metricsClient.recordExecutionTime(Metric.BUCKET_PROCESSING_TIME.getMetricName(), totalDuration);
  }

  /** Record comprehensive batch processing metrics. */
  private void recordBatchProcessingMetrics(
      int batchNumber, ProcessBatchResult result, long batchStartTime) {
    final long batchDuration = System.currentTimeMillis() - batchStartTime;

    metricsClient.recordExecutionTime(
        Metric.BATCH_PROCESSING_TIME.getMetricName(),
        batchDuration,
        MetricTag.BATCH_NUMBER.getTag(String.valueOf(batchNumber)),
        MetricTag.RECORDS_COUNT.getTag(String.valueOf(result.recordsProcessed)));

    metricsClient.count(Metric.BATCH_PROCESSING_COUNT.getMetricName(), 1);
  }

  /** Helper class to hold batch processing results. */
  private static class ProcessBatchResult {
    final List<Transaction> transactionsToSave;
    final List<RetailTransactionLineDTO> overflow;
    final int recordsProcessed;

    ProcessBatchResult(
        List<Transaction> transactionsToSave,
        List<RetailTransactionLineDTO> overflow,
        int recordsProcessed) {
      this.transactionsToSave = transactionsToSave;
      this.overflow = overflow;
      this.recordsProcessed = recordsProcessed;
    }
  }

  /** Cleanup method called before bean destruction. Closes S3 connections to release resources. */
  @PreDestroy
  public void cleanup() {
    try {
      if (s3Utility != null) {
        log.debug("Closing S3 connection using s3Utility");
        s3Utility.closeS3Client();
      }
    } catch (Exception e) {
      log.warn("Error during S3 resource cleanup: {}", e.getMessage(), e);
    }
  }
}
