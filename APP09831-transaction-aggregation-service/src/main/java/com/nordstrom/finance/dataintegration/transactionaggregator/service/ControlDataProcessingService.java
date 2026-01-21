package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.AggregationDBService;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.AggregationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.Metrics;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ControlDataProcessingService {
  private final AggregationDBService aggregationDBService;
  private final MetricsClient metricsClient;
  private final FileProcessingService fileProcessingService;

  /**
   * * Processes control data by generating a control CSV file, saving its details to the database,
   * and uploading it to S3.
   *
   * @param config the aggregation configuration entity
   * @param aggregationFileName the name of the associated aggregation file
   * @param totalAmounts the total amounts to be included in the control file
   * @param aggregationDataRowCount the count of data rows in the aggregation file
   * @throws AggregationException if there is an error during aggregation processing
   * @throws DatabaseConnectionException if there is a database connection error
   * @throws DatabaseOperationException if there is a database operation error
   */
  public void processDataControl(
      AggregationConfigurationEntity config,
      String aggregationFileName,
      String totalAmounts,
      int aggregationDataRowCount)
      throws AggregationException, DatabaseConnectionException, DatabaseOperationException {
    String dataControlFileContent =
        fileProcessingService.getControlDataCSVFileContent(
            config, aggregationFileName, totalAmounts, aggregationDataRowCount);

    String fileName = fileProcessingService.getControlFileName(config.getFileNamePrefix());

    // Persist generated file details
    long dbSaveStartTime = System.currentTimeMillis();
    aggregationDBService.saveGeneratedFileDetails(
        GeneratedFileDetailEntity.builder()
            .generatedFileName(fileName)
            .fileContent(dataControlFileContent)
            .aggregationConfigurationId(config.getAggregationConfigurationId())
            .isUploadedToS3(false)
            .isPublishedToDataPlatform(false)
            .build());
    metricsClient.recordExecutionTime(
        Metrics.CONTROL_FILE_DB_SAVE_TIME.getMetricName(),
        System.currentTimeMillis() - dbSaveStartTime,
        MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()));

    // Upload file to S3
    fileProcessingService.uploadToS3Bucket(dataControlFileContent, fileName);
    metricsClient.incrementCounter(
        Metrics.CONTROL_FILE_GENERATED_COUNT.getMetricName(),
        MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()));

    aggregationDBService.updateGeneratedFileUploadToS3IndicatorToTrue(fileName);

    log.info("Successfully generated control file {} and updated to S3 bucket.", fileName);
  }
}
