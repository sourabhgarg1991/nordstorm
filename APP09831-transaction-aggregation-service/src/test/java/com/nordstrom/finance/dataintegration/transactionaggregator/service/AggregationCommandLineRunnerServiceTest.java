package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getAggregationConfigurationEntity;
import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getAggregationResults;
import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getControlDataResults;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.AggregationDBService;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.AggregationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.Metrics;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsErrorCode;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AggregationCommandLineRunnerService. Tests focus on the service logic with mocked
 * dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AggregationCommandLineRunnerServiceTest {

  @Mock private FileUploadRetryService fileUploadRetryService;
  @Mock private AggregationDBService aggregationDBService;
  @Mock private AggregationDataProcessingService aggregationFileProcessingService;
  @Mock private ControlDataProcessingService controlDataProcessingService;
  @Mock private FileProcessingService fileProcessingService;
  @Mock private MetricsClient metricsClient;

  private AggregationCommandLineRunnerService service;

  @BeforeEach
  void setUp() {
    service =
        new AggregationCommandLineRunnerService(
            fileUploadRetryService,
            aggregationDBService,
            aggregationFileProcessingService,
            controlDataProcessingService,
            fileProcessingService,
            metricsClient);
  }

  /**
   * * Test case for no aggregation configurations. Expects the service to complete without
   * processing any configurations.
   */
  @Test
  void run_noConfigurations_shouldCompleteSuccessfully() throws Exception {
    when(aggregationDBService.getCurrentAggregationConfigurations())
        .thenReturn(Collections.emptyList());

    service.run();
    verify(aggregationDBService).getCurrentAggregationConfigurations();
    verify(fileUploadRetryService, times(1)).retryFileUploads();
    verify(metricsClient, times(1)).count(eq(Metrics.CONFIGURATION_COUNT.getMetricName()), eq(0L));
    verify(metricsClient, times(1))
        .recordExecutionTime(eq(Metrics.JOB_EXECUTION_TIME.getMetricName()), anyLong());
  }

  /*  * Test case for aggregation configuration with no data returned from query.
   * Expects no files to be generated.
   */
  @Test
  void run_withConfiguration_noData_shouldNotUpload() throws Exception {
    AggregationConfigurationEntity config = getAggregationConfigurationEntity();

    when(aggregationDBService.getCurrentAggregationConfigurations()).thenReturn(List.of(config));
    when(aggregationDBService.executeAggregationQuery(anyString()))
        .thenReturn(Collections.emptyList());

    service.run();
    verify(fileUploadRetryService, times(1)).retryFileUploads();
    verify(aggregationDBService).getCurrentAggregationConfigurations();
    verify(aggregationDBService).executeAggregationQuery(config.getAggregationQuery());
    verify(metricsClient, times(1)).count(eq(Metrics.CONFIGURATION_COUNT.getMetricName()), eq(1L));
    verify(metricsClient, times(1))
        .recordExecutionTime(eq(Metrics.JOB_EXECUTION_TIME.getMetricName()), anyLong());
    verify(aggregationFileProcessingService, never())
        .processAggregationData(any(), anyString(), any());
    verify(controlDataProcessingService, never())
        .processDataControl(any(), anyString(), any(), anyInt());
  }

  /**
   * * Test case for successful processing and uploading of aggregation data and control data.
   * Expects files to be generated and metrics to be recorded.
   */
  @Test
  void run_withConfiguration_withData_shouldProcessAndUpload() throws Exception {
    AggregationConfigurationEntity config = getAggregationConfigurationEntity();

    List<String[]> aggregationQueryResults = getAggregationResults();
    String controlDataResults = getControlDataResults();

    when(aggregationDBService.getCurrentAggregationConfigurations()).thenReturn(List.of(config));
    when(aggregationDBService.executeAggregationQuery(any())).thenReturn(aggregationQueryResults);
    when(aggregationDBService.executeControlDataQuery(any())).thenReturn(controlDataResults);

    doNothing().when(aggregationFileProcessingService).processAggregationData(any(), any(), any());
    doNothing()
        .when(controlDataProcessingService)
        .processDataControl(any(), any(), any(), anyInt());

    service.run();
    verify(fileUploadRetryService, times(1)).retryFileUploads();
    verify(aggregationDBService).getCurrentAggregationConfigurations();
    verify(aggregationDBService, times(1)).executeAggregationQuery(anyString());
    verify(aggregationDBService, times(1)).executeControlDataQuery(anyString());
    verify(aggregationFileProcessingService, times(1)).processAggregationData(any(), any(), any());
    verify(controlDataProcessingService, times(1))
        .processDataControl(any(), any(), any(), anyInt());

    verify(metricsClient, times(1)).count(eq(Metrics.CONFIGURATION_COUNT.getMetricName()), eq(1L));
    verify(metricsClient, times(1))
        .count(
            eq(Metrics.AGGREGATION_ROW_COUNT.getMetricName()),
            eq((long) aggregationQueryResults.size()),
            eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));

    verify(metricsClient, times(1))
        .recordExecutionTime(eq(Metrics.JOB_EXECUTION_TIME.getMetricName()), anyLong());

    ArgumentCaptor<Integer> rowCountCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(controlDataProcessingService, times(1))
        .processDataControl(any(), any(), any(), rowCountCaptor.capture());

    // aggregationQueryResults has 3 elements: 1 header + 2 data rows
    // Should pass 2 (not 3) to processDataControl
    assertEquals(2, rowCountCaptor.getValue(), "Row count should exclude header row");
  }

  /**
   * * Test case for multiple aggregation configurations. Expects each configuration to be processed
   * and files generated accordingly.
   */
  @Test
  void multiple_config() throws Exception {
    AggregationConfigurationEntity config = getAggregationConfigurationEntity();

    List<String[]> aggregationQueryResults = getAggregationResults();
    String controlDataResults = getControlDataResults();

    when(aggregationDBService.getCurrentAggregationConfigurations())
        .thenReturn(List.of(config, config));
    when(aggregationDBService.executeAggregationQuery(any())).thenReturn(aggregationQueryResults);
    when(aggregationDBService.executeControlDataQuery(any())).thenReturn(controlDataResults);

    doNothing().when(aggregationFileProcessingService).processAggregationData(any(), any(), any());
    doNothing()
        .when(controlDataProcessingService)
        .processDataControl(any(), any(), any(), anyInt());
    service.run();
    verify(fileUploadRetryService, times(1)).retryFileUploads();
    verify(aggregationDBService).getCurrentAggregationConfigurations();
    verify(aggregationDBService, times(2)).executeAggregationQuery(anyString());
    verify(aggregationDBService, times(2)).executeControlDataQuery(anyString());
    verify(aggregationFileProcessingService, times(2)).processAggregationData(any(), any(), any());
    verify(controlDataProcessingService, times(2))
        .processDataControl(any(), any(), any(), anyInt());

    verify(metricsClient, times(1)).count(eq(Metrics.CONFIGURATION_COUNT.getMetricName()), eq(2L));
    verify(metricsClient, times(2))
        .count(
            eq(Metrics.AGGREGATION_ROW_COUNT.getMetricName()),
            eq((long) aggregationQueryResults.size()),
            eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));

    verify(metricsClient, times(1))
        .recordExecutionTime(eq(Metrics.JOB_EXECUTION_TIME.getMetricName()), anyLong());
  }

  /**
   * Test case where aggregation data is present but control data is missing. Expects an error to be
   * recorded for missing control data. Should not generate files associated to config.
   */
  @Test
  void no_controlData_test() throws Exception {
    AggregationConfigurationEntity config = getAggregationConfigurationEntity();

    List<String[]> aggregationQueryResults = getAggregationResults();
    when(aggregationDBService.getCurrentAggregationConfigurations()).thenReturn(List.of(config));
    when(aggregationDBService.executeAggregationQuery(any())).thenReturn(aggregationQueryResults);
    when(aggregationDBService.executeControlDataQuery(any())).thenReturn(null);
    service.run();
    verify(fileUploadRetryService, times(1)).retryFileUploads();
    verify(aggregationDBService).getCurrentAggregationConfigurations();
    verify(aggregationDBService, times(1)).executeAggregationQuery(anyString());
    verify(aggregationDBService, times(1)).executeControlDataQuery(anyString());
    verify(aggregationFileProcessingService, never()).processAggregationData(any(), any(), any());
    verify(controlDataProcessingService, never()).processDataControl(any(), any(), any(), anyInt());

    verify(metricsClient, times(1)).count(eq(Metrics.CONFIGURATION_COUNT.getMetricName()), eq(1L));
    verify(metricsClient, times(1))
        .count(
            eq(Metrics.AGGREGATION_ROW_COUNT.getMetricName()),
            eq((long) aggregationQueryResults.size()),
            eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
    verify(metricsClient, times(1))
        .incrementErrorCount(
            eq(
                MetricsCommonTag.ERROR_CODE.getTag(
                    MetricsErrorCode.INVALID_DATA_CANNOT_BE_EMPTY.name())),
            eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));

    verify(metricsClient, times(1))
        .recordExecutionTime(eq(Metrics.JOB_EXECUTION_TIME.getMetricName()), anyLong());
  }

  /**
   * * Test case for multiple aggregation configurations with one throwing an exception during
   * processing. Expects the service to continue processing other configurations and record metrics
   * accordingly.
   */
  @Test
  void multiple_config_with_one_exception() throws Exception {
    AggregationConfigurationEntity config = getAggregationConfigurationEntity();

    List<String[]> aggregationQueryResults = getAggregationResults();
    String controlDataResults = getControlDataResults();

    when(aggregationDBService.getCurrentAggregationConfigurations())
        .thenReturn(List.of(config, config));
    when(aggregationDBService.executeAggregationQuery(any())).thenReturn(aggregationQueryResults);
    when(aggregationDBService.executeControlDataQuery(any())).thenReturn(controlDataResults);

    doThrow(new AggregationException("first call fails"))
        .doNothing() // second (and subsequent) calls
        .when(aggregationFileProcessingService)
        .processAggregationData(any(), any(), any());
    doNothing()
        .when(controlDataProcessingService)
        .processDataControl(any(), any(), any(), anyInt());

    service.run();
    verify(fileUploadRetryService, times(1)).retryFileUploads();
    verify(aggregationDBService).getCurrentAggregationConfigurations();
    verify(aggregationDBService, times(2)).executeAggregationQuery(anyString());
    verify(aggregationDBService, times(2)).executeControlDataQuery(anyString());
    verify(aggregationFileProcessingService, times(2)).processAggregationData(any(), any(), any());
    verify(controlDataProcessingService, times(2))
        .processDataControl(any(), any(), any(), anyInt());

    verify(metricsClient, times(1)).count(eq(Metrics.CONFIGURATION_COUNT.getMetricName()), eq(2L));
    verify(metricsClient, times(2))
        .count(
            eq(Metrics.AGGREGATION_ROW_COUNT.getMetricName()),
            eq((long) aggregationQueryResults.size()),
            eq(MetricsTag.FILE_NAME_PREFIX.getTag(config.getFileNamePrefix())));
    verify(metricsClient, times(1))
        .recordExecutionTime(eq(Metrics.JOB_EXECUTION_TIME.getMetricName()), anyLong());
  }

  /** * Test case where an exception occurs during processing. Expects an error to be recorded. */
  @Test
  void run_exceptionDuringProcessing_shouldRecordError() throws Exception {
    when(aggregationDBService.getCurrentAggregationConfigurations())
        .thenThrow(new RuntimeException("Database error"));

    service.run();
    verify(fileUploadRetryService, times(1)).retryFileUploads();
    verify(metricsClient)
        .incrementErrorCount(
            eq(MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.JOB_EXECUTION_ERROR.name())));
  }
}
