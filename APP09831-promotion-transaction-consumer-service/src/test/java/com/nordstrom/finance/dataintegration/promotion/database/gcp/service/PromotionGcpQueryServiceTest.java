package com.nordstrom.finance.dataintegration.promotion.database.gcp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.*;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.config.GcpProperties;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageRequest;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageResponse;
import com.nordstrom.finance.dataintegration.promotion.domain.mapper.BigQueryRowToTransactionDetailMapper;
import com.nordstrom.finance.dataintegration.promotion.exception.PromotionQueryException;
import com.nordstrom.finance.dataintegration.promotion.util.TestSchemaHelper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {PromotionGcpQueryService.class})
@ActiveProfiles("test")
class PromotionGcpQueryServiceTest {
  @Autowired PromotionGcpQueryService service;
  @MockitoBean BigQuery bigQuery;
  @MockitoBean GcpProperties gcpProperties;
  @MockitoBean MetricsClient metricsClient;

  @Test
  void nullQueryJobFailure() throws Exception {
    Job queryJob = Mockito.mock(Job.class);
    when(bigQuery.create(any(JobInfo.class))).thenReturn(queryJob);
    when(queryJob.waitFor()).thenReturn(null);
    assertThrows(
        PromotionQueryException.class,
        () -> service.getPromotionTransactionData(new TransactionPageRequest(null, null)));
  }

  @Test
  void errorStatusFailure() throws Exception {
    Job queryJob = Mockito.mock(Job.class);
    JobStatus jobStatus = Mockito.mock(JobStatus.class);
    when(bigQuery.create(any(JobInfo.class))).thenReturn(queryJob);
    when(queryJob.waitFor()).thenReturn(queryJob);
    when(queryJob.getStatus()).thenReturn(jobStatus);
    when(jobStatus.getError()).thenReturn(new BigQueryError("reason", "location", "message"));
    assertThrows(
        PromotionQueryException.class,
        () -> service.getPromotionTransactionData(new TransactionPageRequest(null, null)));
  }

  @Test
  void sourceRowMappingFailure() throws Exception {
    Job queryJob = Mockito.mock(Job.class);
    JobStatus jobStatus = Mockito.mock(JobStatus.class);
    TableResult tableResult = Mockito.mock(TableResult.class);
    FieldValueList fieldValueList = Mockito.mock(FieldValueList.class);
    Iterable<FieldValueList> values = Collections.singletonList(fieldValueList);

    Schema schema = TestSchemaHelper.getRowSchema();

    when(bigQuery.create(any(JobInfo.class))).thenReturn(queryJob);
    when(queryJob.waitFor()).thenReturn(queryJob);
    when(queryJob.getStatus()).thenReturn(jobStatus);
    when(jobStatus.getError()).thenReturn(null);
    when(queryJob.getQueryResults(any())).thenReturn(tableResult);
    when(tableResult.getValues()).thenReturn(values);
    when(tableResult.getSchema()).thenReturn(schema);

    try (var mocked = Mockito.mockStatic(BigQueryRowToTransactionDetailMapper.class)) {
      mocked
          .when(
              () ->
                  BigQueryRowToTransactionDetailMapper.map(
                      any(FieldValueList.class), any(Schema.class)))
          .thenThrow(new RuntimeException("mapping error"));

      assertThrows(
          PromotionQueryException.class,
          () -> service.getPromotionTransactionData(new TransactionPageRequest(null, null)));
    }
  }

  @Test
  void getPromotionTransactionDataSuccess() throws Exception {
    when(gcpProperties.startDateOverride()).thenReturn("1970-01-01T00:00:00Z");
    when(gcpProperties.endDateOverride()).thenReturn("1970-01-01T00:00:00Z");
    when(gcpProperties.napProjectId()).thenReturn("test-project");
    when(gcpProperties.dataset()).thenReturn("test-dataset");
    when(gcpProperties.tableName()).thenReturn("test-table");
    when(gcpProperties.projectId()).thenReturn("test-project-id");
    when(gcpProperties.pageSize()).thenReturn(1000L);

    Job queryJob = Mockito.mock(Job.class);
    JobStatus jobStatus = Mockito.mock(JobStatus.class);
    TableResult tableResult = Mockito.mock(TableResult.class);

    Schema schema = TestSchemaHelper.getRowSchema();

    FieldValueList row = createRealRowData();

    Iterable<FieldValueList> values = Collections.singletonList(row);

    when(bigQuery.create(any(JobInfo.class))).thenReturn(queryJob);
    when(queryJob.waitFor()).thenReturn(queryJob);
    when(queryJob.getStatus()).thenReturn(jobStatus);
    when(jobStatus.getError()).thenReturn(null);
    when(queryJob.getQueryResults(any())).thenReturn(tableResult);
    when(tableResult.getValues()).thenReturn(values);
    when(tableResult.getSchema()).thenReturn(schema);
    when(tableResult.getNextPageToken()).thenReturn("nextPageToken123");

    TransactionPageResponse response =
        service.getPromotionTransactionData(new TransactionPageRequest(null, null));

    assertNotNull(response);
    assertEquals("nextPageToken123", response.nextPageToken());
    assertNotNull(response.job());
    assertEquals("TXN123456", response.currentPageTransactions().get(0).globalTransactionId());
  }

  /**
   * Creates a real FieldValueList with proper structure for testing. This matches the pattern from
   * BigQueryRowToTransactionDetailMapperTest.
   */
  private FieldValueList createRealRowData() {
    // Create line item detail fields (the STRUCT inside the repeated field)
    FieldValue firstReportedTmstp = Mockito.mock(FieldValue.class);
    when(firstReportedTmstp.getStringValue()).thenReturn("2025-01-15T10:30:00.000000");

    FieldValue businessOrigin = Mockito.mock(FieldValue.class);
    when(businessOrigin.getStringValue()).thenReturn("LOYALTY_PROMO");

    FieldValue lineItemId = Mockito.mock(FieldValue.class);
    when(lineItemId.getStringValue()).thenReturn("LINE123");

    FieldValue discount = Mockito.mock(FieldValue.class);
    when(discount.getNumericValue()).thenReturn(new java.math.BigDecimal("10.50"));

    FieldValue reversalFlag = Mockito.mock(FieldValue.class);
    when(reversalFlag.getStringValue()).thenReturn("N");

    FieldValue tranTypeCode = Mockito.mock(FieldValue.class);
    when(tranTypeCode.getStringValue()).thenReturn("SALE");

    FieldValue activityTypeCode = Mockito.mock(FieldValue.class);
    when(activityTypeCode.getStringValue()).thenReturn("S");

    FieldValue storeNum = Mockito.mock(FieldValue.class);
    when(storeNum.getStringValue()).thenReturn("STORE001");

    List<FieldValue> lineItemFields =
        List.of(
            firstReportedTmstp,
            businessOrigin,
            lineItemId,
            discount,
            reversalFlag,
            tranTypeCode,
            activityTypeCode,
            storeNum);
    FieldValueList lineItemStruct =
        FieldValueList.of(lineItemFields, TestSchemaHelper.getDetailStructFields());

    FieldValue lineItemFieldValue = Mockito.mock(FieldValue.class);
    when(lineItemFieldValue.getAttribute()).thenReturn(FieldValue.Attribute.RECORD);
    when(lineItemFieldValue.getRecordValue()).thenReturn(lineItemStruct);

    // Build the details array (repeated field)
    FieldValue detailsField = Mockito.mock(FieldValue.class);
    when(detailsField.getRepeatedValue()).thenReturn(List.of(lineItemFieldValue));

    // Build business_date and global_tran_id fields
    FieldValue businessDateField = Mockito.mock(FieldValue.class);
    when(businessDateField.getStringValue()).thenReturn("2025-01-15");

    FieldValue globalTranIdField = Mockito.mock(FieldValue.class);
    when(globalTranIdField.getStringValue()).thenReturn("TXN123456");

    List<FieldValue> rowFields = List.of(detailsField, businessDateField, globalTranIdField);
    Schema schema = TestSchemaHelper.getRowSchema();
    return FieldValueList.of(rowFields, schema.getFields());
  }
}
