package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import static com.nordstrom.finance.dataintegration.transactionaggregator.metric.ExecutionStatus.Failure;
import static com.nordstrom.finance.dataintegration.transactionaggregator.metric.ExecutionStatus.Success;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.AggregationDBService;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.AggregationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.Metrics;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsErrorCode;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * * Service class that implements CommandLineRunner to execute aggregation processing at
 * application startup.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AggregationCommandLineRunnerService implements CommandLineRunner {

  private final FileUploadRetryService fileUploadRetryService;
  private final AggregationDBService aggregationDBService;
  private final AggregationDataProcessingService aggregationFileProcessingService;
  private final ControlDataProcessingService controlDataProcessingService;
  private final FileProcessingService fileProcessingService;
  private final MetricsClient metricsClient;

  /**
   * This method is executed after the Spring application context has been fully initialized. It
   * retrieves current aggregation configurations, processes each configuration to generate CSV
   * files, and saves the generated file details and aggregation relations in the database.
   *
   * @param args command line arguments (not used)
   */
  @Transactional
  @Override
  public void run(String... args) {
    long jobStartTime = System.currentTimeMillis();
    try {
      log.info("Aggregation processing started.");

      log.info("started to upload failed generated files if any.");
      fileUploadRetryService.retryFileUploads();
      // Retrieve current aggregation configurations
      List<AggregationConfigurationEntity> configurations =
          aggregationDBService.getCurrentAggregationConfigurations();
      log.info("Retrieved {} current aggregation configurations.", configurations.size());
      metricsClient.count(Metrics.CONFIGURATION_COUNT.getMetricName(), configurations.size());

      // Process each configuration
      for (var config : configurations) {
        try {
          log.info("Processing fileName prefix: {}", config.getFileNamePrefix());
          List<String[]> aggregatedDataRowList =
              aggregationDBService.executeAggregationQuery(config.getAggregationQuery());
          String fileName =
              fileProcessingService.getAggregationFileName(config.getFileNamePrefix());
          if (aggregatedDataRowList == null || aggregatedDataRowList.isEmpty()) {
            log.warn(
                "No aggregation data returned for fileName prefix: {}", config.getFileNamePrefix());
            metricsClient.count(
                Metrics.AGGREGATION_ROW_COUNT.getMetricName(),
                0,
                MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()));
          } else {
            metricsClient.count(
                Metrics.AGGREGATION_ROW_COUNT.getMetricName(),
                aggregatedDataRowList.size(),
                MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()));
            if (StringUtils.hasText(config.getDataControlQuery())) {
              String totalAmount =
                  aggregationDBService.executeControlDataQuery(config.getDataControlQuery());
              if (!StringUtils.hasText(totalAmount)) {
                log.warn(
                    "Data control data cannot be empty when aggregation data is present: {}",
                    config.getFileNamePrefix());
                metricsClient.incrementErrorCount(
                    MetricsCommonTag.ERROR_CODE.getTag(
                        MetricsErrorCode.INVALID_DATA_CANNOT_BE_EMPTY.name()),
                    MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()));
                throw new AggregationException(
                    "Data control data cannot be empty when aggregation data is present.");
              }
              controlDataProcessingService.processDataControl(
                  config, fileName, totalAmount, aggregatedDataRowList.size() - 1);
            } else {
              log.warn(
                  "Data control query is not defined for fileName prefix: {}",
                  config.getFileNamePrefix());
            }
            aggregationFileProcessingService.processAggregationData(
                config, fileName, aggregatedDataRowList);
          }
          metricsClient.incrementCounter(
              Metrics.CONFIGURATION_EXECUTION_STATUS.getMetricName(),
              MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()),
              MetricsTag.STATUS.getTag(Success.name()));
        } catch (Exception e) {
          log.error("Error processing fileName prefix {}.", config.getFileNamePrefix(), e);
          metricsClient.incrementCounter(
              Metrics.CONFIGURATION_EXECUTION_STATUS.getMetricName(),
              MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix()),
              MetricsTag.STATUS.getTag(Failure.name()));
        }
      }
      log.info("Aggregation processing completed successfully.");

    } catch (Exception e) {
      log.error("Error processing aggregation: {}", e.getMessage(), e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.JOB_EXECUTION_ERROR.name()));
    } finally {
      metricsClient.recordExecutionTime(
          Metrics.JOB_EXECUTION_TIME.getMetricName(), System.currentTimeMillis() - jobStartTime);
    }
  }
}
