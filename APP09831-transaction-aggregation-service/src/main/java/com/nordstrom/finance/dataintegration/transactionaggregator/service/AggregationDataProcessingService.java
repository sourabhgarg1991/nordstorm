package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.AggregationDBService;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.TransactionAggregationRelationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.AggregationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.Metrics;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AggregationDataProcessingService {
  private final AggregationDBService aggregationDBService;
  private final MetricsClient metricsClient;
  private final FileProcessingService fileProcessingService;

  public void processAggregationData(
      AggregationConfigurationEntity config, String fileName, List<String[]> aggregatedDataRowList)
      throws AggregationException, DatabaseConnectionException, DatabaseOperationException {
    String aggregationFileContent =
        fileProcessingService.getCSVFileContent(aggregatedDataRowList, config);

    saveTransactionAggregationRelationshipDetails(aggregatedDataRowList);

    // Persist generated file details
    long dbSaveStartTime = System.currentTimeMillis();
    aggregationDBService.saveGeneratedFileDetails(
        GeneratedFileDetailEntity.builder()
            .generatedFileName(fileName)
            .fileContent(aggregationFileContent)
            .isUploadedToS3(false)
            .isPublishedToDataPlatform(false)
            .aggregationConfigurationId(config.getAggregationConfigurationId())
            .build());
    metricsClient.recordExecutionTime(
        Metrics.DB_SAVE_TIME.getMetricName(),
        System.currentTimeMillis() - dbSaveStartTime,
        MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()));

    // Upload file to S3
    fileProcessingService.uploadToS3Bucket(aggregationFileContent, fileName);
    metricsClient.incrementCounter(
        Metrics.FILE_GENERATED_COUNT.getMetricName(),
        MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()));

    aggregationDBService.updateGeneratedFileUploadToS3IndicatorToTrue(fileName);

    log.info("Generated file: {} with {} rows.", fileName, aggregatedDataRowList.size());
  }

  /**
   * * Saves transaction aggregation relationship details into the database.
   *
   * @param dataRowList the list of data rows containing transaction line detail IDs and aggregation
   *     IDs
   * @throws DatabaseConnectionException if there is an issue connecting to the database
   * @throws DatabaseOperationException if there is an error during the database operation
   */
  private void saveTransactionAggregationRelationshipDetails(List<String[]> dataRowList)
      throws DatabaseConnectionException, DatabaseOperationException {
    for (int i = 1; i < dataRowList.size(); i++) {
      String[] row = dataRowList.get(i); // ignore the header row
      List<String> transactionLineDetailIds = Arrays.asList(row[0].split(","));
      String aggregationId = row[1];
      // Generate AggregationDetail objects and save them
      List<TransactionAggregationRelationEntity> aggregationDetailEntities =
          createAggregationDetails(aggregationId, transactionLineDetailIds);
      aggregationDBService.saveTransactionAggregationRelationshipDetails(aggregationDetailEntities);
    }
  }

  /**
   * Creates a list of AggregationDetail objects based on the aggregation ID and transaction line
   * detail IDs.
   *
   * @param aggregationId the aggregation ID
   * @param transactionLineDetailIds the list of transaction line detail IDs
   * @return a list of AggregationDetail objects
   */
  private List<TransactionAggregationRelationEntity> createAggregationDetails(
      String aggregationId, List<String> transactionLineDetailIds) {
    return transactionLineDetailIds.stream()
        .map(
            transactionLineDetailId ->
                TransactionAggregationRelationEntity.builder()
                    .aggregationId(UUID.fromString(aggregationId))
                    .transactionLineId(Long.parseLong(transactionLineDetailId))
                    .isPublishedToDataPlatform(false)
                    .build())
        .toList();
  }
}
