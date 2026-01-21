package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getGeneratedFileDetailEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.AggregationDBService;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsErrorCode;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileUploadRetryServiceTest {
  private FileUploadRetryService fileUploadRetryService;
  @Mock private AggregationDBService aggregationDBService;
  @Mock private FileProcessingService fileProcessingService;
  @Mock private MetricsClient metricsClient;

  @BeforeEach
  void setUp() {
    fileUploadRetryService =
        new FileUploadRetryService(aggregationDBService, fileProcessingService, metricsClient);
  }

  @Test
  void testRetryFileUploads() {
    try {
      when(aggregationDBService.getGeneratedFileToBeUploadToS3())
          .thenReturn(List.of(getGeneratedFileDetailEntity()));
      fileUploadRetryService.retryFileUploads();
      verify(fileProcessingService, times(1)).uploadToS3Bucket(any(), any());
      verify(aggregationDBService, times(1)).updateGeneratedFileUploadToS3IndicatorToTrue(any());
      verify(metricsClient, never())
          .incrementErrorCount(
              eq(
                  MetricsCommonTag.ERROR_CODE.getTag(
                      MetricsErrorCode.RETRY_FILE_UPLOAD_ERROR.name())),
              eq(
                  MetricsTag.FILE_NAME.getTag(
                      getGeneratedFileDetailEntity().getGeneratedFileName())));
    } catch (Exception e) {
      assert false : e.getMessage();
    }
  }

  @Test
  void testRetryFileUploads_Error() {
    try {
      doThrow(new DatabaseConnectionException("DB Error"))
          .when(aggregationDBService)
          .getGeneratedFileToBeUploadToS3();

      fileUploadRetryService.retryFileUploads();
      verify(fileProcessingService, never()).uploadToS3Bucket(any(), any());
      verify(aggregationDBService, never()).updateGeneratedFileUploadToS3IndicatorToTrue(any());
      verify(metricsClient, times(1))
          .incrementErrorCount(
              eq(
                  MetricsCommonTag.ERROR_CODE.getTag(
                      MetricsErrorCode.RETRY_FILE_UPLOAD_PROCESS_EXECUTION_ERROR.name())));
    } catch (Exception e) {
      assert false : e.getMessage();
    }
  }

  @Test
  void testRetryFileUploads_NoFile() {
    try {
      when(aggregationDBService.getGeneratedFileToBeUploadToS3()).thenReturn(List.of());
      fileUploadRetryService.retryFileUploads();
      verify(fileProcessingService, never()).uploadToS3Bucket(any(), any());
      verify(aggregationDBService, never()).updateGeneratedFileUploadToS3IndicatorToTrue(any());
      verify(metricsClient, never())
          .incrementErrorCount(
              eq(
                  MetricsCommonTag.ERROR_CODE.getTag(
                      MetricsErrorCode.RETRY_FILE_UPLOAD_ERROR.name())),
              anyString());
    } catch (Exception e) {
      assert false : e.getMessage();
    }
  }
}
