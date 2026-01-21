package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getAggregationConfigurationEntity;
import static com.nordstrom.finance.dataintegration.transactionaggregator.service.TestDataUtility.getAggregationResults;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nordstrom.finance.dataintegration.common.aws.S3Utility;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.metric.MetricsErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileProcessingServiceTest {
  @Mock private MetricsClient metricsClient;
  @Mock private S3Utility s3Utility;
  private FileProcessingService fileProcessingService;

  @BeforeEach
  void setUp() {
    fileProcessingService = new FileProcessingService(metricsClient, s3Utility);
  }

  @Test
  public void testGetAggregationFileName() {
    String prefix = "TestPrefix";
    String fileName = fileProcessingService.getAggregationFileName(prefix);
    assert fileName.startsWith(prefix + "_");
    assert fileName.endsWith(".csv");
  }

  @Test
  public void testGetControlFileName() {
    String prefix = "TestPrefix";
    String fileName = fileProcessingService.getControlFileName(prefix);
    assert fileName.startsWith(prefix + "_CONTROL_");
    assert fileName.endsWith(".csv");
  }

  @Test
  public void testGetCSVFileContent() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();
      List<String[]> inputData = getAggregationResults();
      String csvContent = fileProcessingService.getCSVFileContent(inputData, config);
      assertEquals(
          "aggregation_id|data_source|amount|BusinessDate\r\n"
              + "550e8400-e29b-41d4-a716-446655440000|RETAIL|10.90|2025/10/10\r\n"
              + "550e8400-e29b-41d4-a716-446655441111|RETAIL|10.00|2025/10/10\r\n",
          csvContent);
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  public void testGetCSVFileContent_WithQuotes() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();
      config.setIsDataQuotesSurrounded(true);
      List<String[]> inputData = getAggregationResults();
      String csvContent = fileProcessingService.getCSVFileContent(inputData, config);
      assertEquals(
          "\"aggregation_id\"|\"data_source\"|\"amount\"|\"BusinessDate\"\r\n"
              + "\"550e8400-e29b-41d4-a716-446655440000\"|\"RETAIL\"|\"10.90\"|\"2025/10/10\"\r\n"
              + "\"550e8400-e29b-41d4-a716-446655441111\"|\"RETAIL\"|\"10.00\"|\"2025/10/10\"\r\n",
          csvContent);
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  public void testGetCSVFileContent_NoDataRows() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();
      String csvContent = fileProcessingService.getCSVFileContent(List.of(), config);
      assertEquals("", csvContent);
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  public void testGetControlDataCSVFileContent() {
    try {
      AggregationConfigurationEntity config = getAggregationConfigurationEntity();
      String controlDataAmount = "100.00";
      int recordCount = 5;
      String csvContent =
          fileProcessingService.getControlDataCSVFileContent(
              config, "TestControlFile.csv", controlDataAmount, recordCount);
      assertEquals(
          "FileName|TotalAmount|AggregationDataRowCount\r\n" + "TestControlFile.csv|100.00|5\r\n",
          csvContent);
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  public void writeToFile_successTest() {
    try {
      String content = "Test content";
      String filePath = "TestFile.txt";
      when(s3Utility.uploadFile(any(), any(), any())).thenReturn(true);
      fileProcessingService.uploadToS3Bucket(content, filePath);
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  public void writeToFile_failureTest() {
    try {
      String content = "Test content";
      String filePath = "TestFile.txt";
      when(s3Utility.uploadFile(any(), any(), any())).thenReturn(false);
      fileProcessingService.uploadToS3Bucket(content, filePath);
      verify(metricsClient, times(1))
          .incrementErrorCount(
              eq(MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.S3_UPLOAD_ERROR.name())),
              anyString(),
              anyString());
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }
}
