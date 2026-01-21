package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getAggregationConfigurationEntity;
import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getAggregationResults;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.AggregationDBService;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.Metrics;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AggregationDataProcessingServiceTest {
  private AggregationDataProcessingService aggregationDataProcessingService;
  @Mock private AggregationDBService aggregationDBService;
  @Mock private FileProcessingService fileProcessingService;
  @Mock private MetricsClient metricsClient;

  @BeforeEach
  void setUp() {
    aggregationDataProcessingService =
        new AggregationDataProcessingService(
            aggregationDBService, metricsClient, fileProcessingService);
  }

  @Test
  void testProcessAggregationData() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();
      List<String[]> aggregationQueryResults = getAggregationResults();
      aggregationDataProcessingService.processAggregationData(
          config, "test.CSV", aggregationQueryResults);
      verify(fileProcessingService, never()).getAggregationFileName(any());
      verify(fileProcessingService, times(1)).getCSVFileContent(any(), any());
      verify(fileProcessingService, times(1)).uploadToS3Bucket(any(), any());
      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());
      verify(aggregationDBService, times(aggregationQueryResults.size() - 1))
          .saveTransactionAggregationRelationshipDetails(any());
      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());
      verify(metricsClient, times(1))
          .recordExecutionTime(
              eq(Metrics.DB_SAVE_TIME.getMetricName()),
              anyLong(),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
      verify(metricsClient, times(1))
          .incrementCounter(
              eq(Metrics.FILE_GENERATED_COUNT.getMetricName()),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  void testProcessAggregationData_DBException() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();
      List<String[]> aggregationQueryResults = getAggregationResults();

      doThrow(new DatabaseConnectionException("DB Error"))
          .when(aggregationDBService)
          .saveGeneratedFileDetails(any());

      assertThrows(
          DatabaseConnectionException.class,
          () -> {
            aggregationDataProcessingService.processAggregationData(
                config, "test.CSV", aggregationQueryResults);
          });
      verify(fileProcessingService, never()).getAggregationFileName(any());
      verify(fileProcessingService, times(1)).getCSVFileContent(any(), any());
      verify(fileProcessingService, never()).uploadToS3Bucket(any(), any());
      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());
      verify(aggregationDBService, times(aggregationQueryResults.size() - 1))
          .saveTransactionAggregationRelationshipDetails(any());
      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());
      verify(metricsClient, never())
          .recordExecutionTime(
              eq(Metrics.DB_SAVE_TIME.getMetricName()),
              anyLong(),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
      verify(metricsClient, never())
          .incrementCounter(
              eq(Metrics.FILE_GENERATED_COUNT.getMetricName()),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }
}
