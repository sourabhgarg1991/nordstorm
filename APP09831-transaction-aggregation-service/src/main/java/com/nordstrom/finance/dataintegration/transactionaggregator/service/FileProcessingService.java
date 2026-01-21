package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import static com.nordstrom.finance.dataintegration.transactionaggregator.metric.ExecutionStatus.Failure;
import static com.nordstrom.finance.dataintegration.transactionaggregator.metric.ExecutionStatus.Success;

import com.nordstrom.finance.dataintegration.common.aws.S3Utility;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.AggregationException;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.Metrics;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsErrorCode;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsTag;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileProcessingService {
  private final MetricsClient metricsClient;
  private final S3Utility s3Utility;
  final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

  @Value("${aws.s3.bucket.upload}")
  private String UPLOAD_BUCKET_NAME;

  /**
   * * Generates a unique aggregation file name using the provided prefix and the current timestamp.
   *
   * @param fileNamePrefix the prefix for the file name
   * @return the generated aggregation file name
   */
  public String getAggregationFileName(String fileNamePrefix) {
    return fileNamePrefix + "_" + LocalDateTime.now().format(YYYY_MM_DD_HH_MM_SS) + ".csv";
  }

  /**
   * * Generates a unique control file name using the provided prefix and the current timestamp.
   *
   * @param fileNamePrefix the prefix for the file name
   * @return the generated control file name
   */
  public String getControlFileName(String fileNamePrefix) {
    return fileNamePrefix + "_CONTROL_" + LocalDateTime.now().format(YYYY_MM_DD_HH_MM_SS) + ".csv";
  }

  /**
   * Generates CSV file content from the provided data rows and aggregation configuration.
   *
   * @param dataRowList the list of data rows to be included in the CSV file
   * @param aggregationConfiguration the aggregation configuration entity containing file settings
   * @return the generated CSV file content as a string
   * @throws AggregationException if an error occurs during CSV generation
   */
  public String getCSVFileContent(
      List<String[]> dataRowList, AggregationConfigurationEntity aggregationConfiguration)
      throws AggregationException {
    try {
      StringWriter stringWriter = new StringWriter();
      CSVFormat.Builder csvFormatBuilder =
          CSVFormat.DEFAULT.builder().setDelimiter(aggregationConfiguration.getFileDelimiter());

      if (aggregationConfiguration.getIsDataQuotesSurrounded()) {
        csvFormatBuilder.setQuote('"');
        csvFormatBuilder.setQuoteMode(QuoteMode.ALL);
      } else {
        csvFormatBuilder.setQuote(null);
      }
      CSVFormat csvFormat = csvFormatBuilder.get();

      try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, csvFormat)) {
        for (String[] row : dataRowList) {
          List<String> rowWithoutFirstColumn =
              Arrays.asList(row).subList(1, row.length); // ignore transactionLineDetailIds
          csvPrinter.printRecord(rowWithoutFirstColumn);
        }
      }
      return stringWriter.toString();
    } catch (IOException e) {
      log.error("Error generating CSV content: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.CSV_GENERATION_ERROR.name()),
          MetricsTag.FILE_NAME.getTag(aggregationConfiguration.getFileNamePrefix()));
      throw new AggregationException("Error generating CSV content.");
    }
  }

  /**
   * * Generates CSV file content for control data from the provided parameters and aggregation
   * configuration.
   *
   * @param aggregationConfiguration the aggregation configuration entity containing file settings
   * @param aggregationFileName the name of the aggregation file
   * @param totalAmounts the total amounts as a string
   * @param aggregationDataRowCount the count of aggregation data rows
   * @return the generated CSV file content as a string
   * @throws AggregationException if an error occurs during CSV generation
   */
  public String getControlDataCSVFileContent(
      AggregationConfigurationEntity aggregationConfiguration,
      String aggregationFileName,
      String totalAmounts,
      int aggregationDataRowCount)
      throws AggregationException {
    try {
      StringWriter stringWriter = new StringWriter();
      CSVFormat.Builder csvFormatBuilder =
          CSVFormat.DEFAULT.builder().setDelimiter(aggregationConfiguration.getFileDelimiter());

      if (aggregationConfiguration.getIsDataQuotesSurrounded()) {
        csvFormatBuilder.setQuote('"');
        csvFormatBuilder.setQuoteMode(QuoteMode.ALL);
      } else {
        csvFormatBuilder.setQuote(null);
      }
      CSVFormat csvFormat = csvFormatBuilder.get();

      try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, csvFormat)) {
        csvPrinter.printRecord(List.of("FileName", "TotalAmount", "AggregationDataRowCount"));
        csvPrinter.printRecord(
            List.of(aggregationFileName, totalAmounts, String.valueOf(aggregationDataRowCount)));
      }
      return stringWriter.toString();
    } catch (IOException e) {
      log.error("Error generating CSV content: {}", e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.CSV_GENERATION_ERROR.name()),
          MetricsTag.FILE_NAME.getTag(aggregationConfiguration.getFileNamePrefix()));
      throw new AggregationException("Error generating CSV content.");
    }
  }

  /**
   * Writes the given content to a file in the specified S3 bucket.
   *
   * @param content the content to write to the file
   * @param fileName the name of the file to be created in the S3 bucket
   */
  public void uploadToS3Bucket(String content, String fileName) {
    try {
      long s3UploadStartTime = System.currentTimeMillis();

      boolean uploadSuccess = s3Utility.uploadFile(content, fileName, UPLOAD_BUCKET_NAME);

      metricsClient.incrementCounter(
          Metrics.S3_UPLOAD_STATUS.getMetricName(),
          MetricsTag.FILE_NAME.getTag(fileName),
          MetricsTag.STATUS.getTag(uploadSuccess ? Success.name() : Failure.name()));

      metricsClient.recordExecutionTime(
          Metrics.S3_UPLOAD_TIME.getMetricName(),
          System.currentTimeMillis() - s3UploadStartTime,
          MetricsTag.FILE_NAME.getTag(fileName),
          MetricsTag.BUCKET_NAME.getTag(UPLOAD_BUCKET_NAME));
      if (uploadSuccess) {
        log.info("File:{} uploaded successfully to S3 bucket: {}", fileName, UPLOAD_BUCKET_NAME);
      } else {
        log.error("Failed to upload file:{} to S3 bucket: {}", fileName, UPLOAD_BUCKET_NAME);
        metricsClient.incrementErrorCount(
            MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.S3_UPLOAD_ERROR.name()),
            MetricsTag.FILE_NAME.getTag(fileName),
            MetricsTag.BUCKET_NAME.getTag(UPLOAD_BUCKET_NAME));
      }
    } catch (Exception e) {
      log.error("Error writing file to {}: {}", fileName, e.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.S3_UPLOAD_ERROR.name()),
          MetricsTag.FILE_NAME.getTag(fileName));
      throw e;
    }
  }

  /**
   * Cleanup method called before the bean is destroyed. Closes the S3 client to release resources.
   */
  @PreDestroy
  public void cleanup() {
    if (s3Utility != null) {
      s3Utility.closeS3Client();
    }
  }
}
