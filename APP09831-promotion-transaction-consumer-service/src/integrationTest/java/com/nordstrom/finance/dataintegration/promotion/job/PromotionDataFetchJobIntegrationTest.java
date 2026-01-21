package com.nordstrom.finance.dataintegration.promotion.job;

import static com.nordstrom.finance.dataintegration.promotion.util.SourceRowTestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.*;
import com.nordstrom.finance.dataintegration.promotion.config.TestBigQueryConfiguration;
import com.nordstrom.finance.dataintegration.promotion.config.TestMetricsConfiguration;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.PromotionTransactionLine;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.Transaction;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.repository.TransactionRepository;
import com.nordstrom.finance.dataintegration.promotion.util.EntityAssertionHelper;
import com.nordstrom.finance.dataintegration.promotion.util.TableResultBuilder;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
@Import({TestBigQueryConfiguration.class, TestMetricsConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Promotion Data Fetch Job Integration Tests")
public class PromotionDataFetchJobIntegrationTest {

  @Autowired private PromotionDataFetchJobService jobService;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private BigQuery bigQuery;
  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  private Job queryJob;
  private JobStatus jobStatus;

  private static final String TEST_LINE_ITEM_ID = "LINE_001";
  private static final String TEST_STORE_NUM = "0100";
  private static final String SOURCE_SYSTEM_TYPE = "GCP";
  private static final int ASYNC_WAIT_SECONDS = 5;

  @BeforeEach
  public void setup() throws InterruptedException {
    transactionTemplate.execute(
        status -> {
          List<Transaction> existingTransactions =
              transactionRepository.findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE);
          if (!existingTransactions.isEmpty()) {
            transactionRepository.deleteAll(existingTransactions);
          }

          entityManager.flush();
          entityManager.clear();

          return null;
        });

    // Reset mocks for each test
    queryJob = Mockito.mock(Job.class);
    jobStatus = Mockito.mock(JobStatus.class);
    Mockito.reset(bigQuery);
    when(bigQuery.create(any(JobInfo.class))).thenReturn(queryJob);
    when(queryJob.waitFor()).thenReturn(queryJob);
    when(queryJob.getStatus()).thenReturn(jobStatus);
    when(jobStatus.getError()).thenReturn(null);
  }

  @AfterAll
  public void cleanup() {
    // Clean up test data after all tests complete
    transactionTemplate.execute(
        status -> {
          List<Transaction> existingTransactions =
              transactionRepository.findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE);
          if (!existingTransactions.isEmpty()) {
            transactionRepository.deleteAll(existingTransactions);
          }

          entityManager.flush();
          entityManager.clear();

          return null;
        });
  }

  /**
   * Helper method to wait for async processing completion and retrieve transactions. Uses
   * Awaitility for more robust async handling than Thread.sleep. Executes within a transaction to
   * handle lazy loading.
   */
  private List<Transaction> runJobAndWaitForTransactions(TableResult tableResult, int expectedCount)
      throws Exception {
    when(queryJob.getQueryResults(any())).thenReturn(tableResult);

    jobService.run();

    // Wait for async processing to complete with proper polling
    Awaitility.await()
        .atMost(ASYNC_WAIT_SECONDS, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .until(
            () -> {
              entityManager.clear();
              // Only count transactions with the expected sourceReferenceSystemType
              return transactionRepository
                  .findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE)
                  .size();
            },
            count -> count == expectedCount);

    return transactionTemplate.execute(
        status -> {
          entityManager.clear();
          List<Transaction> transactions =
              transactionRepository.findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE);
          transactions.forEach(
              t -> {
                t.getTransactionLines().size();
                t.getTransactionLines().forEach(tl -> tl.getPromotionTransactionLines().size());
              });
          return transactions;
        });
  }

  /** Helper method for scenarios where we expect no transactions. */
  private void runJobAndExpectNoTransactions(TableResult tableResult) throws Exception {
    when(queryJob.getQueryResults(any())).thenReturn(tableResult);

    jobService.run();

    Thread.sleep(500);
    entityManager.clear();

    List<Transaction> transactions =
        transactionRepository.findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE);
    assertThat(transactions).isEmpty();
  }

  static Stream<Arguments> loyaltyScenarios() {
    return Stream.of(
        args(
            "Loyalty #1: SALE/S/N",
            loyaltyScenario1(),
            "TEST_GLOBAL_TRAN_L01",
            "SALE",
            "SALE",
            "N",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #2: SALE/S/Y",
            loyaltyScenario2(),
            "TEST_GLOBAL_TRAN_L02",
            "SALE",
            "SALE",
            "Y",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #3: RETN/R/N",
            loyaltyScenario3(),
            "TEST_GLOBAL_TRAN_L03",
            "RETN",
            "RETN",
            "N",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #4: RETN/R/Y",
            loyaltyScenario4(),
            "TEST_GLOBAL_TRAN_L04",
            "RETN",
            "RETN",
            "Y",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #5: RETN/S/N",
            loyaltyScenario5(),
            "TEST_GLOBAL_TRAN_L05",
            "RETN",
            "SALE",
            "N",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #6: EXCH/S/N",
            loyaltyScenario6(),
            "TEST_GLOBAL_TRAN_L06",
            "EXCH",
            "SALE",
            "N",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #7: EXCH/S/Y",
            loyaltyScenario7(),
            "TEST_GLOBAL_TRAN_L07",
            "EXCH",
            "SALE",
            "Y",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #8: EXCH/R/N",
            loyaltyScenario8(),
            "TEST_GLOBAL_TRAN_L08",
            "EXCH",
            "RETN",
            "N",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #9: EXCH/R/Y",
            loyaltyScenario9(),
            "TEST_GLOBAL_TRAN_L09",
            "EXCH",
            "RETN",
            "Y",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #10: VOID/S/N",
            loyaltyScenario10(),
            "TEST_GLOBAL_TRAN_L10",
            "VOID",
            "SALE",
            "N",
            5.00,
            "LOYALTY_PROMO"),
        args(
            "Loyalty #11: VOID/R/N",
            loyaltyScenario11(),
            "TEST_GLOBAL_TRAN_L11",
            "VOID",
            "RETN",
            "N",
            5.00,
            "LOYALTY_PROMO"));
  }

  static Stream<Arguments> marketingScenarios() {
    return Stream.of(
        args(
            "Marketing #1: SALE/S/N",
            marketingScenario1(),
            "TEST_GLOBAL_TRAN_M01",
            "SALE",
            "SALE",
            "N",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #2: SALE/S/Y",
            marketingScenario2(),
            "TEST_GLOBAL_TRAN_M02",
            "SALE",
            "SALE",
            "Y",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #3: RETN/R/N",
            marketingScenario3(),
            "TEST_GLOBAL_TRAN_M03",
            "RETN",
            "RETN",
            "N",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #4: RETN/R/Y",
            marketingScenario4(),
            "TEST_GLOBAL_TRAN_M04",
            "RETN",
            "RETN",
            "Y",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #5: RETN/S/N",
            marketingScenario5(),
            "TEST_GLOBAL_TRAN_M05",
            "RETN",
            "SALE",
            "N",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #6: EXCH/S/N",
            marketingScenario6(),
            "TEST_GLOBAL_TRAN_M06",
            "EXCH",
            "SALE",
            "N",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #7: EXCH/S/Y",
            marketingScenario7(),
            "TEST_GLOBAL_TRAN_M07",
            "EXCH",
            "SALE",
            "Y",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #8: EXCH/R/N",
            marketingScenario8(),
            "TEST_GLOBAL_TRAN_M08",
            "EXCH",
            "RETN",
            "N",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #9: EXCH/R/Y",
            marketingScenario9(),
            "TEST_GLOBAL_TRAN_M09",
            "EXCH",
            "RETN",
            "Y",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #10: VOID/S/N",
            marketingScenario10(),
            "TEST_GLOBAL_TRAN_M10",
            "VOID",
            "SALE",
            "N",
            5.00,
            "MARKETING_PROMO"),
        args(
            "Marketing #11: VOID/R/N",
            marketingScenario11(),
            "TEST_GLOBAL_TRAN_M11",
            "VOID",
            "RETN",
            "N",
            5.00,
            "MARKETING_PROMO"));
  }

  private static Arguments args(
      String name,
      FieldValueList data,
      String globalTranId,
      String tranType,
      String lineType,
      String reversal,
      double amount,
      String origin) {
    return Arguments.of(
        name, data, globalTranId, tranType, lineType, reversal, BigDecimal.valueOf(amount), origin);
  }

  @Test
  @DisplayName("Should process single transaction successfully")
  public void shouldProcessSingleTransaction() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);

    assertThat(transactions).hasSize(1);
    assertThat(transactions.get(0).getSourceReferenceTransactionId())
        .isEqualTo("TEST_GLOBAL_TRAN_L01");
    assertThat(transactions.get(0).getTransactionLines()).hasSize(1);
  }

  @Test
  @DisplayName("Should process multiple transactions in batch")
  public void shouldProcessMultipleTransactions() throws Exception {
    TableResult result =
        TableResultBuilder.buildWithRows(
            withCustomGlobalTranId("TRAN_001"),
            withCustomGlobalTranId("TRAN_002"),
            withCustomGlobalTranId("TRAN_003"));

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 3);
    assertThat(transactions).hasSize(3);
  }

  @Test
  @DisplayName("Should handle empty result set")
  public void shouldHandleEmptyResultSet() throws Exception {
    TableResult result = TableResultBuilder.buildEmpty();
    runJobAndExpectNoTransactions(result);
  }

  @Test
  @DisplayName("Should skip duplicate transactions")
  public void shouldSkipDuplicateTransactions() throws Exception {
    TableResult tableResult = TableResultBuilder.buildWithRows(loyaltyScenario1());

    List<Transaction> firstRun = runJobAndWaitForTransactions(tableResult, 1);
    assertThat(firstRun).hasSize(1);

    when(queryJob.getQueryResults(any())).thenReturn(tableResult);
    jobService.run();
    Thread.sleep(500);

    entityManager.clear();
    List<Transaction> secondRun =
        transactionRepository.findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE);
    assertThat(secondRun).hasSize(1);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("loyaltyScenarios")
  @DisplayName("Loyalty Scenarios")
  public void shouldProcessLoyaltyScenarios(
      String scenario,
      FieldValueList data,
      String globalTranId,
      String tranType,
      String lineType,
      String reversal,
      BigDecimal amount,
      String origin)
      throws Exception {
    assertScenario(data, globalTranId, tranType, lineType, reversal, amount, origin);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("marketingScenarios")
  @DisplayName("Marketing Scenarios")
  public void shouldProcessMarketingScenarios(
      String scenario,
      FieldValueList data,
      String globalTranId,
      String tranType,
      String lineType,
      String reversal,
      BigDecimal amount,
      String origin)
      throws Exception {
    assertScenario(data, globalTranId, tranType, lineType, reversal, amount, origin);
  }

  private void assertScenario(
      FieldValueList data,
      String globalTranId,
      String tranType,
      String lineType,
      String reversal,
      BigDecimal amount,
      String origin)
      throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(data);

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    Transaction transaction = transactions.get(0);
    EntityAssertionHelper.assertTransaction(transaction, globalTranId, tranType, reversal);

    assertThat(transaction.getTransactionLines()).hasSize(1);
    TransactionLine line = transaction.getTransactionLines().get(0);
    EntityAssertionHelper.assertTransactionLine(line, TEST_LINE_ITEM_ID, lineType);

    assertThat(line.getPromotionTransactionLines()).hasSize(1);
    PromotionTransactionLine promoLine = line.getPromotionTransactionLines().get(0);
    EntityAssertionHelper.assertPromotionTransactionLine(promoLine, amount, origin);
  }

  @Test
  @DisplayName("Scenario #23: Multiple line items")
  public void shouldHandleMultipleLineItems() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(multipleLineItems());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    Transaction transaction = transactions.get(0);
    assertThat(transaction.getSourceReferenceTransactionId())
        .isEqualTo("TEST_GLOBAL_TRAN_MULTI_LINE");
    assertThat(transaction.getTransactionLines()).hasSize(2);

    List<String> lineIds =
        transaction.getTransactionLines().stream()
            .map(TransactionLine::getSourceReferenceLineId)
            .toList();
    assertThat(lineIds).containsExactlyInAnyOrder("LINE_001", "LINE_002");
  }

  @Test
  @DisplayName("Scenario #24: Multiple stores")
  public void shouldHandleMultipleStores() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(multipleStores());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    Transaction transaction = transactions.get(0);
    assertThat(transaction.getSourceReferenceTransactionId())
        .isEqualTo("TEST_GLOBAL_TRAN_MULTI_STORE");
    assertThat(transaction.getTransactionLines()).hasSize(2);

    List<String> stores =
        transaction.getTransactionLines().stream().map(TransactionLine::getStoreOfIntent).toList();
    assertThat(stores).containsExactlyInAnyOrder("0100", "0101");
  }

  @Test
  @DisplayName("Should maintain bidirectional relationships")
  public void shouldMaintainBidirectionalRelationships() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    Transaction transaction = transactions.get(0);
    TransactionLine line = transaction.getTransactionLines().get(0);
    PromotionTransactionLine promoLine = line.getPromotionTransactionLines().get(0);

    assertThat(line.getTransaction()).isEqualTo(transaction);
    assertThat(promoLine.getTransactionLine()).isEqualTo(line);
  }

  @Test
  @DisplayName("Should cascade persist with generated IDs")
  public void shouldCascadePersistChildEntities() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(multipleLineItems());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    Transaction transaction = transactions.get(0);
    EntityAssertionHelper.assertAllEntitiesPersisted(transaction);
  }

  @Test
  @DisplayName("Should correctly map Transaction fields")
  public void shouldMapTransactionFields() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    Transaction transaction = transactions.get(0);
    assertThat(transaction.getSourceReferenceTransactionId()).isEqualTo("TEST_GLOBAL_TRAN_L01");
    assertThat(transaction.getSourceReferenceSystemType()).isEqualTo("GCP");
    assertThat(transaction.getSourceReferenceType()).isEqualTo("PROMO");
    assertThat(transaction.getTransactionType()).isEqualTo("SALE");
    assertThat(transaction.getTransactionReversalCode()).isEqualTo("N");
    assertThat(transaction.getSourceProcessedDate()).isNotNull();
    assertThat(transaction.getBusinessDate()).isNotNull();
    assertThat(transaction.getPartnerRelationshipType()).isNull();
  }

  @Test
  @DisplayName("Should correctly map TransactionLine fields")
  public void shouldMapTransactionLineFields() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    TransactionLine line = transactions.get(0).getTransactionLines().get(0);
    assertThat(line.getSourceReferenceLineId()).isEqualTo(TEST_LINE_ITEM_ID);
    assertThat(line.getSourceReferenceLineType()).isEqualTo("PROMO");
    assertThat(line.getTransactionLineType()).isEqualTo("SALE");
    assertThat(line.getStoreOfIntent()).isEqualTo(TEST_STORE_NUM);
  }

  @Test
  @DisplayName("Should correctly map PromotionTransactionLine fields")
  public void shouldMapPromotionLineFields() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 1);
    assertThat(transactions).hasSize(1);

    PromotionTransactionLine promoLine =
        transactions.get(0).getTransactionLines().get(0).getPromotionTransactionLines().get(0);
    assertThat(promoLine.getPromoType()).isNull();
    assertThat(promoLine.getPromoAmount()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
    assertThat(promoLine.getPromoBusinessOrigin()).isEqualTo("LOYALTY_PROMO");
  }

  @Test
  @DisplayName("Should handle BigQuery creation failure")
  public void shouldHandleBigQueryCreationFailure() {
    when(bigQuery.create(any(JobInfo.class))).thenThrow(new RuntimeException("Connection failed"));

    assertThrows(Exception.class, () -> jobService.run());
    assertThat(transactionRepository.findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE)).isEmpty();
  }

  @Test
  @DisplayName("Should handle BigQuery execution error")
  public void shouldHandleBigQueryExecutionError() {
    when(jobStatus.getError())
        .thenReturn(new BigQueryError("ERROR", "Query failed", "Invalid SQL"));

    assertThrows(Exception.class, () -> jobService.run());
    assertThat(transactionRepository.findBySourceReferenceSystemType(SOURCE_SYSTEM_TYPE)).isEmpty();
  }

  @Test
  @DisplayName("Should handle reversal flags (Y/N)")
  public void shouldHandleReversalFlags() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1(), loyaltyScenario2());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 2);

    List<String> reversalCodes =
        transactions.stream().map(Transaction::getTransactionReversalCode).toList();
    assertThat(reversalCodes).containsExactlyInAnyOrder("N", "Y");
  }

  @Test
  @DisplayName("Should convert all discounts to absolute values")
  public void shouldHandleDiscounts() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1(), loyaltyScenario3());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 2);

    List<BigDecimal> amounts =
        transactions.stream()
            .flatMap(t -> t.getTransactionLines().stream())
            .flatMap(l -> l.getPromotionTransactionLines().stream())
            .map(PromotionTransactionLine::getPromoAmount)
            .toList();
    assertThat(amounts).allMatch(amt -> amt.compareTo(BigDecimal.valueOf(5.00)) == 0);
  }

  @Test
  @DisplayName("Should handle all transaction types")
  public void shouldHandleTransactionTypes() throws Exception {
    TableResult result =
        TableResultBuilder.buildWithRows(
            loyaltyScenario1(), loyaltyScenario3(), loyaltyScenario6(), loyaltyScenario10());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 4);

    List<String> types = transactions.stream().map(Transaction::getTransactionType).toList();
    assertThat(types).containsExactlyInAnyOrder("SALE", "RETN", "EXCH", "VOID");
  }

  @Test
  @DisplayName("Should handle both business origins")
  public void shouldHandleBusinessOrigins() throws Exception {
    TableResult result = TableResultBuilder.buildWithRows(loyaltyScenario1(), marketingScenario1());

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 2);

    List<String> origins =
        transactions.stream()
            .flatMap(t -> t.getTransactionLines().stream())
            .flatMap(l -> l.getPromotionTransactionLines().stream())
            .map(PromotionTransactionLine::getPromoBusinessOrigin)
            .toList();
    assertThat(origins).containsExactlyInAnyOrder("LOYALTY_PROMO", "MARKETING_PROMO");
  }

  @Test
  @DisplayName("Should handle large batch efficiently")
  public void shouldHandleLargeBatch() throws Exception {
    List<FieldValueList> rows = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      rows.add(withCustomGlobalTranId(String.format("TRAN_%03d", i)));
    }
    TableResult result = TableResultBuilder.buildWithRows(rows);

    List<Transaction> transactions = runJobAndWaitForTransactions(result, 100);
    assertThat(transactions).hasSize(100);
  }
}
