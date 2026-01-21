package com.nordstrom.finance.dataintegration.transactionaggregator.service;

import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import java.time.LocalDate;
import java.util.List;

public class TestDataUtility {
  private TestDataUtility() {}

  public static AggregationConfigurationEntity getAggregationConfigurationEntity() {
    return AggregationConfigurationEntity.builder()
        .aggregationConfigurationId(1L)
        .fileNamePrefix("Test_Aggregation")
        .fileDelimiter('|')
        .isDataQuotesSurrounded(false)
        .aggregationQuery("SELECT * FROM test")
        .dataControlQuery("SELECT count(*) FROM test")
        .startDate(LocalDate.now().minusDays(1))
        .build();
  }

  public static List<String[]> getAggregationResults() {
    return List.of(
        new String[] {
          "transaction_line_ids", "aggregation_id", "data_source", "amount", "BusinessDate"
        },
        new String[] {
          "1,2,3", "550e8400-e29b-41d4-a716-446655440000", "RETAIL", "10.90", "2025/10/10"
        },
        new String[] {
          "1,2,4", "550e8400-e29b-41d4-a716-446655441111", "RETAIL", "10.00", "2025/10/10"
        });
  }

  public static String getControlDataResults() {
    return "20.90";
  }

  public static String getAggregationCVSFileContent() {
    return "aggregation_id|data_source|amount|BusinessDate\r\n"
        + "550e8400-e29b-41d4-a716-446655440000|RETAIL|10.90|2025/10/10\r\n"
        + "550e8400-e29b-41d4-a716-446655441111|RETAIL|10.00|2025/10/10\r\n";
  }

  public static GeneratedFileDetailEntity getGeneratedFileDetailEntity() {
    return GeneratedFileDetailEntity.builder()
        .generatedFileName("Test_CONTROL.CSV")
        .fileContent("control_data_amount|BusinessDate\r\n20.90|2025/10/10\r\n")
        .isUploadedToS3(false)
        .isPublishedToDataPlatform(false)
        .aggregationConfigurationId(1L)
        .build();
  }
}
