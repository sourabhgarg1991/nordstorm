package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.AggregationDBService;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsErrorCode;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadRetryService {
  private final AggregationDBService aggregationDBService;
  private final FileProcessingService fileProcessingService;
  private final MetricsClient metricsClient;

  public void retryFileUploads() {
    try {
      List<GeneratedFileDetailEntity> filesToRetry =
          aggregationDBService.getGeneratedFileToBeUploadToS3();
      if (CollectionUtils.isEmpty(filesToRetry)) {
        log.info("S3 file upload retry complete: 0 failed files found.");
        return;
      }
      log.info("Found {} files to retry upload to S3.", filesToRetry.size());
      for (GeneratedFileDetailEntity fileDetail : filesToRetry) {
        try {
          fileProcessingService.uploadToS3Bucket(
              fileDetail.getFileContent(), fileDetail.getGeneratedFileName());
          aggregationDBService.updateGeneratedFileUploadToS3IndicatorToTrue(
              fileDetail.getGeneratedFileName());
        } catch (Exception e) {
          log.error("Failed to upload file: " + fileDetail.getGeneratedFileName(), e);
          metricsClient.incrementErrorCount(
              MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.RETRY_FILE_UPLOAD_ERROR.name()),
              MetricsTag.FILE_NAME.getTag(fileDetail.getGeneratedFileName()));
        }
      }

    } catch (Exception e) {
      log.error("Failed to retrieve generated FileDetails for retry upload", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              MetricsErrorCode.RETRY_FILE_UPLOAD_PROCESS_EXECUTION_ERROR.name()));
    }
    log.info("S3 file upload retry process completed.");
  }
}
