package com.nordstrom.finance.dataintegration.ertm.consumer;

import com.nordstrom.finance.dataintegration.ertm.consumer.config.TestMetricsConfiguration;
import com.nordstrom.finance.dataintegration.ertm.consumer.config.TestS3Configuration;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.ertm.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.ertm.exception.FileMappingException;
import com.nordstrom.finance.dataintegration.ertm.service.FileProcessorService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@ActiveProfiles("integrationTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {"spring.config.location = classpath:application-integrationtest.yml"})
@Import({TestMetricsConfiguration.class, TestS3Configuration.class})
public class RetailTransactionIntegrationTest {
  @Autowired TransactionDBService transactionDBService;
  @Autowired FileProcessorService fileProcessorService;

  @BeforeEach
  public void setUp() throws SQLException {
    deleteAuroraDbTestData();
  }

  BufferedReader readCsvFile(String fileName) throws IOException {
    InputStream inputStream =
        RetailTransactionIntegrationTest.class.getClassLoader().getResourceAsStream(fileName);
    return new BufferedReader(new InputStreamReader(inputStream));
  }

  List<Transaction> fetchTransactionDataFromDB() {
    List<Transaction> transactionList = transactionDBService.getAll();
    return transactionList.stream()
        .filter(transaction -> ("ertm").equals(transaction.getSourceReferenceSystemType()))
        .toList();
  }

  @Order(1)
  @Test()
  public void testBatchData_success_validData() throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");
    fileProcessorService.processFileData(reader);

    List<Transaction> transactionList = fetchTransactionDataFromDB();
    Transaction transaction = transactionList.getFirst();

    assert null != transaction.getId();
    assert transactionList.size() == 1;
    assert transaction.getSourceReferenceTransactionId().equals("1");
    assert transaction.getCreatedDateTime() != null;
    assert transaction.getLastUpdatedDateTime() != null;
    assert transaction
        .getTransactionDate()
        .equals(LocalDate.parse("2023-04-26T04:46:57-08:00".substring(0, 10)));
    assert transaction.getBusinessDate().equals(LocalDate.parse("2023-06-15"));
    assert transaction.getSourceProcessedDate().equals(LocalDate.parse("2022-12-12"));
    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("0808");
    assert transaction.getTransactionType().equals("SALE");
    assert transaction.getTransactionReversalCode().equals("N");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getSourceReferenceLineType()
        .equals("Tender");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("64.95"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("0");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("RR");
    assert transaction.getSourceReferenceType().equals("retail");
    assert transaction.getTransactionLines().getFirst().getTransactionLineType() == null;
  }

  @Order(2)
  @Test()
  public void testBatchData_success_nullData() throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Null_Data.csv");
    fileProcessorService.processFileData(reader);
    List<Transaction> transactionList = fetchTransactionDataFromDB();

    assert transactionList.isEmpty();
  }

  @Order(3)
  @Test()
  public void testBatchData_success_transaction_different_files()
      throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");
    fileProcessorService.processFileData(reader);

    reader = readCsvFile("ertm_source/ERTM_Existing_Transaction.csv");
    fileProcessorService.processFileData(reader);

    List<Transaction> transactions = fetchTransactionDataFromDB();
    assert !transactions.isEmpty();
    assert transactions.size() == 2;
    assert transactions.getFirst().getTransactionLines().size() == 1;
    Transaction transaction = transactions.getFirst();
    assert transaction.getCreatedDateTime() != null;
    assert transaction.getLastUpdatedDateTime() != null;
    assert transaction.getSourceReferenceTransactionId().equals("1");
    assert transaction
        .getTransactionDate()
        .equals(LocalDate.parse("2023-04-26T04:46:57-08:00".substring(0, 10)));
    assert transaction.getBusinessDate().equals(LocalDate.parse("2023-06-15"));
    assert transaction.getSourceProcessedDate().equals(LocalDate.parse("2022-12-12"));
    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("0808");
    assert transaction.getTransactionType().equals("SALE");
    assert transaction.getTransactionReversalCode().equals("N");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getSourceReferenceLineType()
        .equals("Tender");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("64.95"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("0");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("RR");

    transaction = transactions.get(1);

    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("0808");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getSourceReferenceLineType()
        .equals("Tender");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("79"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("1");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("RR");
  }

  @Order(4)
  @Test()
  public void testBatchData_success_transaction_already_exist()
      throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");
    fileProcessorService.processFileData(reader);

    reader = readCsvFile("ertm_source/ERTM_Valid_Data.csv");
    fileProcessorService.processFileData(reader);

    List<Transaction> transactions = fetchTransactionDataFromDB();
    assert !transactions.isEmpty();
    assert transactions.size() == 1;
    assert transactions.getFirst().getTransactionLines().size() == 1;
    Transaction transaction = transactions.getFirst();

    assert transaction.getSourceReferenceTransactionId().equals("1");
    assert transaction.getCreatedDateTime() != null;
    assert transaction.getLastUpdatedDateTime() != null;
    assert transaction
        .getTransactionDate()
        .equals(LocalDate.parse("2023-04-26T04:46:57-08:00".substring(0, 10)));
    assert transaction.getBusinessDate().equals(LocalDate.parse("2023-06-15"));
    assert transaction.getSourceProcessedDate().equals(LocalDate.parse("2022-12-12"));
    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("0808");
    assert transaction.getTransactionType().equals("SALE");
    assert transaction.getTransactionReversalCode().equals("N");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getSourceReferenceLineType()
        .equals("Tender");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("64.95"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("0");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("RR");
  }

  @Order(5)
  @Test()
  public void testBatchData_success_transaction_multiple_data()
      throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Valid_Multi_Data.csv");
    fileProcessorService.processFileData(reader);

    List<Transaction> transactions = fetchTransactionDataFromDB();
    assert !transactions.isEmpty();
    assert transactions.size() == 2;
    assert transactions.getFirst().getTransactionLines().size() == 2;
    assert transactions.getLast().getTransactionLines().size() == 1;
    Transaction transaction = transactions.getFirst();
    transaction
        .getTransactionLines()
        .sort(Comparator.comparing(TransactionLine::getSourceReferenceLineId));
    assert transaction.getCreatedDateTime() != null;
    assert transaction.getLastUpdatedDateTime() != null;
    assert transaction.getSourceReferenceTransactionId().equals("1");
    assert transaction
        .getTransactionDate()
        .equals(LocalDate.parse("2023-04-26T04:46:57-08:00".substring(0, 10)));
    assert transaction.getBusinessDate().equals(LocalDate.parse("2023-06-15"));
    assert transaction.getSourceProcessedDate().equals(LocalDate.parse("2022-12-12"));
    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("0808");
    assert transaction.getTransactionType().equals("SALE");
    assert transaction.getTransactionReversalCode().equals("N");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getSourceReferenceLineType()
        .equals("Tender");
    assert transaction.getTransactionLines().getFirst().getSourceReferenceLineId().equals("101");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("64.95"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("0");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("RR");

    assert transaction.getTransactionLines().get(1).getRingingStore().equals("0910");
    assert transaction.getTransactionLines().get(1).getSourceReferenceLineType().equals("Tender");
    assert transaction
        .getTransactionLines()
        .getLast()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("70"));
    assert transaction.getTransactionLines().getLast().getSourceReferenceLineId().equals("102");
    assert transaction
        .getTransactionLines()
        .getLast()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getLast()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("1");
    assert transaction
        .getTransactionLines()
        .getLast()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("AA");

    transaction = transactions.getLast();

    assert transaction.getSourceReferenceTransactionId().equals("2");
    assert transaction
        .getTransactionDate()
        .equals(LocalDate.parse("2023-04-26T04:46:57-08:00".substring(0, 10)));
    assert transaction.getBusinessDate().equals(LocalDate.parse("2023-06-15"));
    assert transaction.getSourceProcessedDate().equals(LocalDate.parse("2022-12-12"));
    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("1000");
    assert transaction.getTransactionType().equals("Return");
    assert transaction.getTransactionReversalCode().equals("N");
    assert transaction.getTransactionLines().getFirst().getSourceReferenceLineType().equals("NA");
    assert transaction.getTransactionLines().getFirst().getSourceReferenceLineId().equals("103");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("81"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("1");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("SS");
  }

  @Order(6)
  @Test()
  public void testBatchData_dataSourceCode_RPOS() throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_Data_Source_RPOS.csv");
    fileProcessorService.processFileData(reader);

    List<Transaction> transactionList = fetchTransactionDataFromDB();
    Transaction transaction = transactionList.getFirst();

    assert null != transaction.getId();
    assert transactionList.size() == 1;
    assert transaction.getSourceReferenceTransactionId().equals("1");
    assert transaction
        .getTransactionDate()
        .equals(LocalDate.parse("2023-04-26T04:46:57-08:00".substring(0, 10)));
    assert transaction.getCreatedDateTime() != null;
    assert transaction.getLastUpdatedDateTime() != null;
    assert transaction.getBusinessDate().equals(LocalDate.parse("2023-06-15"));
    assert transaction.getSourceProcessedDate().equals(LocalDate.parse("2022-12-12"));
    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("0808");
    assert transaction.getTransactionType().equals("SALE");
    assert transaction.getTransactionReversalCode().equals("N");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getSourceReferenceLineType()
        .equals("Tender");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("64.95"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("0");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("RR");
    assert transaction.getSourceReferenceType().equals("retail");
  }

  @Order(7)
  @Test()
  public void testBatchData_success_negative_amount() throws IOException, FileMappingException {
    BufferedReader reader = readCsvFile("ertm_source/ERTM_negative_amount.csv");
    fileProcessorService.processFileData(reader);

    List<Transaction> transactionList = fetchTransactionDataFromDB();
    Transaction transaction = transactionList.getFirst();

    assert null != transaction.getId();
    assert transactionList.size() == 1;
    assert transaction.getSourceReferenceTransactionId().equals("101");
    assert transaction.getCreatedDateTime() != null;
    assert transaction.getLastUpdatedDateTime() != null;
    assert transaction
        .getTransactionDate()
        .equals(LocalDate.parse("2023-04-26T04:46:57-08:00".substring(0, 10)));
    assert transaction.getBusinessDate().equals(LocalDate.parse("2023-06-15"));
    assert transaction.getSourceProcessedDate().equals(LocalDate.parse("2022-12-12"));
    assert transaction.getTransactionLines().getFirst().getRingingStore().equals("0808");
    assert transaction.getTransactionType().equals("SALE");
    assert transaction.getTransactionReversalCode().equals("N");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getSourceReferenceLineType()
        .equals("Tender");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderAmount()
        .equals(new BigDecimal("-64.95"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getWaivedAmount()
        .equals(new BigDecimal("-10.10"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getEmployeeDiscountAmount()
        .equals(new BigDecimal("-12.30"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTaxAmount()
        .equals(new BigDecimal("-6.10"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getLineItemAmount()
        .equals(new BigDecimal("-20.45"));
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderType()
        .equals("NC");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardTypeCode()
        .equals("0");
    assert transaction
        .getTransactionLines()
        .getFirst()
        .getRetailTransactionLine()
        .getTenderCardSubTypeCode()
        .equals("RR");
    assert transaction.getSourceReferenceType().equals("retail");
    assert transaction.getTransactionLines().getFirst().getTransactionLineType() == null;
  }

  private void deleteAuroraDbTestData() throws SQLException {
    final String DELETE_QUERY =
        "DELETE FROM transaction WHERE SOURCE_REFERENCE_SYSTEM_TYPE = 'ertm'";

    try (Connection connection = getAuroraDbConnection();
        PreparedStatement psDeleteTestData = connection.prepareStatement(DELETE_QUERY)) {
      psDeleteTestData.executeUpdate();
    } catch (Exception e) {
      log.error("Failed to delete Aurora DB Test Data for retail transaction");
    }
  }

  private Connection getAuroraDbConnection() throws ClassNotFoundException, SQLException {
    Class.forName("org.postgresql.Driver");

    return DriverManager.getConnection(
        "jdbc:postgresql://"
            + System.getenv("AURORA_TEST_POSTGRESQL_DATABASE_ENDPOINT")
            + ":5432/"
            + System.getenv("AURORA_TEST_DATABASE_NAME"),
        System.getenv("AURORA_TEST_POSTGRESQL_USERNAME"),
        System.getenv("AURORA_TEST_POSTGRESQL_PASSWORD"));
  }

  @AfterAll
  public void cleanup() throws SQLException {
    deleteAuroraDbTestData();
  }
}
