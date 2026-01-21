package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getAggregationConfigurationEntity;
import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getControlDataResults;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ControlDataProcessingServiceTest {
  private ControlDataProcessingService controlDataProcessingService;
  @Mock private AggregationDBService aggregationDBService;
  @Mock private FileProcessingService fileProcessingService;
  @Mock private MetricsClient metricsClient;

  @BeforeEach
  void setUp() {
    controlDataProcessingService =
        new ControlDataProcessingService(
            aggregationDBService, metricsClient, fileProcessingService);
  }

  @Test
  void testProcessAggregationData() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();
      String controlDataAmount = getControlDataResults();
      controlDataProcessingService.processDataControl(
          config, "testAggregation.CSV", controlDataAmount, 2);
      verify(fileProcessingService, times(1)).getControlFileName(any());
      verify(fileProcessingService, times(1))
          .getControlDataCSVFileContent(any(), any(), any(), anyInt());
      verify(fileProcessingService, times(1)).uploadToS3Bucket(any(), any());
      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());

      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());
      verify(metricsClient, times(1))
          .recordExecutionTime(
              eq(Metrics.CONTROL_FILE_DB_SAVE_TIME.getMetricName()),
              anyLong(),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
      verify(metricsClient, times(1))
          .incrementCounter(
              eq(Metrics.CONTROL_FILE_GENERATED_COUNT.getMetricName()),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  void testProcessAggregationData_DBException() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();

      doThrow(new DatabaseConnectionException("DB Error"))
          .when(aggregationDBService)
          .saveGeneratedFileDetails(any());

      assertThrows(
          DatabaseConnectionException.class,
          () -> {
            controlDataProcessingService.processDataControl(
                config, "testAggregation.CSV", getControlDataResults(), 2);
          });
      verify(fileProcessingService, times(1)).getControlFileName(any());
      verify(fileProcessingService, times(1))
          .getControlDataCSVFileContent(any(), any(), any(), anyInt());
      verify(fileProcessingService, never()).uploadToS3Bucket(any(), any());
      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());

      verify(aggregationDBService, times(1)).saveGeneratedFileDetails(any());
      verify(metricsClient, never())
          .recordExecutionTime(
              eq(Metrics.CONTROL_FILE_DB_SAVE_TIME.getMetricName()),
              anyLong(),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
      verify(metricsClient, never())
          .incrementCounter(
              eq(Metrics.CONTROL_FILE_GENERATED_COUNT.getMetricName()),
              eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }
}
