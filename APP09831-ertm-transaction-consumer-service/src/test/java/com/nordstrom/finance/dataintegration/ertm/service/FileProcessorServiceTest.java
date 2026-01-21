package com.nordstrom.finance.dataintegration.ertm.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import com.nordstrom.finance.dataintegration.common.aws.S3Utility;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.ertm.config.AwsServiceConfig;
import com.nordstrom.finance.dataintegration.ertm.database.entity.RetailTransactionLine;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.ertm.exception.DataSourceExtractException;
import com.nordstrom.finance.dataintegration.ertm.exception.FileMappingException;
import com.nordstrom.finance.dataintegration.ertm.mapper.RecordGenerator;
import com.nordstrom.finance.dataintegration.ertm.mapper.RetailTransactionLineMapper;
import com.opencsv.exceptions.CsvException;
import java.io.*;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@SpringBootTest(classes = {FileProcessorService.class})
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class FileProcessorServiceTest {

  @Autowired private FileProcessorService fileProcessorService;

  @MockitoBean private MetricsClient metricsClient;
  @MockitoBean private S3Utility s3Utility;
  @MockitoBean private AwsServiceConfig awsServiceConfig;
  @MockitoBean private TransactionDBService transactionDBService;
  @MockitoBean private RetailTransactionLineMapper retailTransactionLineMapper;
  @Mock private software.amazon.awssdk.services.s3.model.S3Object mockS3Object;
  @Mock private ResponseInputStream<GetObjectResponse> mockS3InputStream;
  @MockitoBean BufferedReader mockBufferedReader;
  @MockitoBean InputStream mockInputStream;
  RetailTransactionLine retailTransactionLine;

  @BeforeEach
  public void setup() throws FileNotFoundException {
    when(awsServiceConfig.getSourceBucket()).thenReturn("bucket");
    retailTransactionLine = RecordGenerator.generateFullRetailTransaction();
  }

  @Test
  public void processCsvFromS3Test_success() throws IOException, DataSourceExtractException {
    try (MockedStatic<S3Utility> mockedS3Util = Mockito.mockStatic(S3Utility.class)) {
      when(transactionDBService.getExistingLineItemIds(anyString(), anyList()))
          .thenReturn(Collections.emptyList());
      when(mockBufferedReader.readLine()).thenReturn("first line");
      when(s3Utility.downloadFileAsStream(any(), eq("bucket"))).thenReturn(mockS3InputStream);
      when(s3Utility.listFileKeys("bucket")).thenReturn(List.of("file_1"));
      fileProcessorService.processCsvFromS3();

      verify(s3Utility, times(1)).downloadFileAsStream(any(), eq("bucket"));
      verify(s3Utility, times(1)).listFileKeys(eq("bucket"));
    }
  }

  @Test
  public void processCsvFromS3Test_duplicate()
      throws FileNotFoundException, DataSourceExtractException {
    try (MockedStatic<S3Utility> mockedS3Util = Mockito.mockStatic(S3Utility.class)) {
      when(s3Utility.downloadFileAsStream(any(), eq("bucket"))).thenReturn(mockS3InputStream);
      when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(any(), any()))
          .thenReturn(retailTransactionLine);
      when(s3Utility.listFileKeys("bucket")).thenReturn(List.of("file_1"));
      fileProcessorService.processCsvFromS3();

      verify(s3Utility, times(1)).downloadFileAsStream(any(), eq("bucket"));
      verify(s3Utility, times(1)).listFileKeys(eq("bucket"));
    }
  }

  @Test
  public void processBatch_success() throws IOException, CsvException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");

    when(transactionDBService.getExistingLineItemIds(eq("1"), anyList()))
        .thenReturn(Collections.emptyList());
    doNothing().when(transactionDBService).saveTransaction(any(Transaction.class));
    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(any(), any()))
        .thenReturn(retailTransactionLine);
    fileProcessorService.processFileData(reader);

    verify(retailTransactionLineMapper, times(1)).mapRecordToRetailTransactionLine(any(), any());
    verify(transactionDBService, times(1)).saveAllTransaction(any());
  }

  @Test
  public void processBatch_duplicate_transactionline() throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");

    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(any(), any()))
        .thenReturn(retailTransactionLine);
    when(transactionDBService.getExistingTransactionIds(anyList())).thenReturn(List.of("1"));
    when(transactionDBService.getExistingLineItemIds(anyString(), anyList()))
        .thenReturn(List.of("101"));
    fileProcessorService.processFileData(reader);

    verify(retailTransactionLineMapper, times(0)).mapRecordToRetailTransactionLine(any(), any());
  }

  @Test
  public void processBatch_existing_transaction() throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");

    Transaction transaction = RecordGenerator.generateTransaction();
    when(transactionDBService.getExistingLineItemIds(anyString(), anyList()))
        .thenReturn(Collections.emptyList());
    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(
            any(), any(Transaction.class)))
        .thenReturn(mock(RetailTransactionLine.class));
    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(any(), any()))
        .thenReturn(retailTransactionLine);
    fileProcessorService.processFileData(reader);

    verify(retailTransactionLineMapper, times(1)).mapRecordToRetailTransactionLine(any(), any());
    verify(transactionDBService, times(1)).saveAllTransaction(any());
  }

  @Test
  public void processBatch_no_existing_transaction() throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");

    Transaction transaction = RecordGenerator.generateTransaction();
    when(transactionDBService.getExistingLineItemIds(anyString(), anyList()))
        .thenReturn(Collections.emptyList());
    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(
            any(), any(Transaction.class)))
        .thenReturn(mock(RetailTransactionLine.class));
    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(any(), any()))
        .thenReturn(retailTransactionLine);
    fileProcessorService.processFileData(reader);

    verify(retailTransactionLineMapper, times(1)).mapRecordToRetailTransactionLine(any(), any());
    verify(transactionDBService, times(1)).saveAllTransaction(any());
  }

  @Test
  public void processBatch_validateBatchingAndTransactionGrouping()
      throws IOException, FileMappingException {
    // Create test data with multiple transactions across batch boundaries
    String csvContent = createTestCsvContent();
    BufferedReader reader = new BufferedReader(new StringReader(csvContent));

    // Mock no duplicates
    when(transactionDBService.getExistingLineItemIds(anyString(), anyList()))
        .thenReturn(Collections.emptyList());

    // Mock retail transaction line mapper
    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(any(), any()))
        .thenReturn(retailTransactionLine);

    // Process the file
    fileProcessorService.processFileData(reader);

    // Verify that saveAllTransaction was called multiple times (for multiple batches)
    // With batch size 10 and 25 records, we expect at least 2 batch saves
    verify(transactionDBService, atLeast(2)).saveAllTransaction(any());

    // Verify that transaction grouping happened (mapper called for each transaction line)
    verify(retailTransactionLineMapper, times(25)).mapRecordToRetailTransactionLine(any(), any());
  }

  @Test
  public void processBatch_validateTransactionBoundaries()
      throws IOException, FileMappingException {
    // Create test data where transactions span across batch boundaries
    String csvContent = createTestCsvWithTransactionBoundaries();
    BufferedReader reader = new BufferedReader(new StringReader(csvContent));

    // Mock no duplicates
    when(transactionDBService.getExistingLineItemIds(anyString(), anyList()))
        .thenReturn(Collections.emptyList());

    // Mock retail transaction line mapper
    when(retailTransactionLineMapper.mapRecordToRetailTransactionLine(any(), any()))
        .thenReturn(retailTransactionLine);

    // Process the file
    fileProcessorService.processFileData(reader);

    // Verify that all lines were processed eventually
    verify(retailTransactionLineMapper, times(15)).mapRecordToRetailTransactionLine(any(), any());

    // Verify multiple batch saves occurred
    verify(transactionDBService, times(2)).saveAllTransaction(any());
  }

  /**
   * Creates test CSV content with 25 records across 5 transactions (5 lines each) This should
   * result in multiple batches with batch size 10
   */
  private String createTestCsvContent() {
    StringBuilder csv = new StringBuilder();

    // Copy the header format from the existing working CSV file
    csv.append(
        "\"SOURCE_REFERENCE_TRANSACTION_ID\"|\"TRANSACTION_DATE\"|\"BUSINESS_DATE\"|\"SOURCE_PROCESSED_DATE\"|\"RINGING_STORE\"|\"TRANSACTION_TYPE\"|\"TRANSACTION_REVERSAL_CODE\"|\"SOURCE_REFERENCE_LINE_TYPE\"|\"SOURCE_REFERENCE_LINE_ID\"|\"TRANSACTION_LINE_TYPE\"|\"STORE_OF_INTENT\"|\"DEPARTMENT_ID\"|\"CLASS_ID\"|\"FEE_CODE\"|\"STORE_CHARGE_FLAG\"|\"LEGACY_GL_STORE\"|\"CASH_DISBURSEMENT_LINE1\"|\"CASH_DISBURSEMENT_LINE2\"|\"FULFILLMENT_TYPE_DROPSHIP_CODE\"|\"WAIVED_REASON_CODE\"|\"LINE_ITEM_AMOUNT\"|\"TAX_AMOUNT\"|\"EMPLOYEE_DISCOUNT_AMOUNT\"|\"WAIVED_AMOUNT\"|\"TENDER_TYPE\"|\"TENDER_CARD_TYPE\"|\"TENDER_CARD_SUBTYPE\"|\"TENDER_ADJUSTMENT_CODE\"|\"TENDER_AMOUNT\"|\"DATA_SOURCE_CODE\"\n");

    String[] transactionIds = {"TXN001", "TXN002", "TXN003", "TXN004", "TXN005"};

    for (String txnId : transactionIds) {
      for (int lineNum = 1; lineNum <= 5; lineNum++) {
        csv.append(
            String.format(
                "%s|2023-04-26|2023-06-15|2022-12-12|808|SALE|N   |Tender|%s_%d||||||||||||||||NC|0|RR||64.95|Retail%n",
                txnId, txnId, lineNum));
      }
    }
    return csv.toString();
  }

  /**
   * Creates test CSV content with transactions that span batch boundaries 15 records with large
   * transactions that will cross batch size 10
   */
  private String createTestCsvWithTransactionBoundaries() {
    StringBuilder csv = new StringBuilder();

    // Copy the header format from the existing working CSV file
    csv.append(
        "\"SOURCE_REFERENCE_TRANSACTION_ID\"|\"TRANSACTION_DATE\"|\"BUSINESS_DATE\"|\"SOURCE_PROCESSED_DATE\"|\"RINGING_STORE\"|\"TRANSACTION_TYPE\"|\"TRANSACTION_REVERSAL_CODE\"|\"SOURCE_REFERENCE_LINE_TYPE\"|\"SOURCE_REFERENCE_LINE_ID\"|\"TRANSACTION_LINE_TYPE\"|\"STORE_OF_INTENT\"|\"DEPARTMENT_ID\"|\"CLASS_ID\"|\"FEE_CODE\"|\"STORE_CHARGE_FLAG\"|\"LEGACY_GL_STORE\"|\"CASH_DISBURSEMENT_LINE1\"|\"CASH_DISBURSEMENT_LINE2\"|\"FULFILLMENT_TYPE_DROPSHIP_CODE\"|\"WAIVED_REASON_CODE\"|\"LINE_ITEM_AMOUNT\"|\"TAX_AMOUNT\"|\"EMPLOYEE_DISCOUNT_AMOUNT\"|\"WAIVED_AMOUNT\"|\"TENDER_TYPE\"|\"TENDER_CARD_TYPE\"|\"TENDER_CARD_SUBTYPE\"|\"TENDER_ADJUSTMENT_CODE\"|\"TENDER_AMOUNT\"|\"DATA_SOURCE_CODE\"\n");

    // Transaction 1: 8 lines (fits in first batch)
    for (int i = 1; i <= 8; i++) {
      csv.append(
          String.format(
              "TXN_LARGE_001|2023-04-26|2023-06-15|2022-12-12|808|SALE|N   |Tender|TXN_LARGE_001_%d||||||||||||||||NC|0|RR||64.95|Retail%n",
              i));
    }

    // Transaction 2: 4 lines (starts in first batch, continues in second batch)
    for (int i = 1; i <= 4; i++) {
      csv.append(
          String.format(
              "TXN_LARGE_002|2023-04-26|2023-06-15|2022-12-12|808|SALE|N   |Tender|TXN_LARGE_002_%d||||||||||||||||NC|0|RR||64.95|Retail%n",
              i));
    }

    // Transaction 3: 3 lines (fits in second batch)
    for (int i = 1; i <= 3; i++) {
      csv.append(
          String.format(
              "TXN_LARGE_003|2023-04-26|2023-06-15|2022-12-12|808|SALE|N   |Tender|TXN_LARGE_003_%d||||||||||||||||NC|0|RR||64.95|Retail%n",
              i));
    }

    return csv.toString();
  }

  BufferedReader readCsvFile(String fileName) throws IOException {
    InputStream inputStream =
        FileProcessorServiceTest.class.getClassLoader().getResourceAsStream(fileName);
    return new BufferedReader(new InputStreamReader(inputStream));
  }
}
