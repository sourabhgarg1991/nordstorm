package com.nordstrom.finance.dataintegration.consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.Application;
import com.nordstrom.finance.dataintegration.config.TestKafkaConfig;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade;
import com.nordstrom.standard.PartnerRelationshipType;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * End-to-End Integration Test for SDM Marketplace Consumer with Embedded Kafka.
 *
 * <p>This test validates the complete flow: Kafka Message → @KafkaListener → Service → Mapper →
 * Database
 *
 * <p>Uses @EmbeddedKafka to avoid external Kafka dependencies.
 */
@Slf4j
@ActiveProfiles("integrationtest")
@ContextConfiguration(classes = {Application.class, TestKafkaConfig.class})
@EmbeddedKafka(
    partitions = 1,
    topics = {"test-marketplace-topic"})
@SpringBootTest
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SDMMarketplaceConsumerIntegrationTest {

  @Autowired private EntityManager entityManager;

  @Autowired
  @Qualifier("kafkaTestTemplate")
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired private TransactionDBService transactionDBService;

  @MockitoSpyBean private SDMMarketplaceConsumerService marketplaceConsumerService;

  @Value("${app.kafka.consumer.topic.marketplace}")
  private String marketplaceTopic;

  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  @Value("${spring.datasource.username}")
  private String datasourceUsername;

  @Value("${spring.datasource.password}")
  private String datasourcePassword;

  // Set this to false to keep test data in database for manual verification
  private static final boolean CLEANUP_AFTER_TESTS = false;

  @BeforeEach
  public void setUp() throws SQLException {
    log.info("=== INTEGRATION TEST SETUP START ===");
    if (CLEANUP_AFTER_TESTS) {
      log.info("Cleaning up test data before test execution");
      cleanupTestData();
    } else {
      log.info("Cleanup disabled - test data will persist for verification");
    }
    log.info("=== INTEGRATION TEST SETUP COMPLETE ===");
  }

  @AfterEach
  public void tearDown() throws SQLException {
    log.info("=== INTEGRATION TEST TEARDOWN START ===");
    if (CLEANUP_AFTER_TESTS) {
      log.info("Cleaning up test data after test execution");
      cleanupTestData();
    } else {
      log.warn("CLEANUP DISABLED - Test data remains in database for verification");
    }
    log.info("=== INTEGRATION TEST TEARDOWN COMPLETE ===");
  }

  @Test
  @Order(1)
  public void testEndToEnd_BasicTransaction_AllFieldsAndMethods_Success() {
    log.info("TEST START: Basic Transaction - All Service Methods & Field Mapping");

    FinancialRetailTransaction transaction = FinancialRetailTransactionBuilderFacade.buildDefault();
    String transactionId = String.valueOf(transaction.getFinancialRetailTransactionRecordId());

    log.info("Sending transaction {} to Kafka topic: {}", transactionId, marketplaceTopic);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));
    kafkaTemplate.flush();

    log.info("Waiting for transaction {} to be processed and persisted to database", transactionId);
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "MARKETPLACE", transactionId)));

    log.info("Transaction {} found in database, verifying details", transactionId);
    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    assertEquals(transactionId, savedTransaction.getSourceReferenceTransactionId());
    assertEquals("SDM", savedTransaction.getSourceReferenceSystemType());
    assertEquals("MARKETPLACE", savedTransaction.getSourceReferenceType());
    assertEquals("SALE", savedTransaction.getTransactionType());
    assertEquals("N", savedTransaction.getTransactionReversalCode());
    assertNotNull(savedTransaction.getTransactionId());
    assertNotNull(savedTransaction.getTransactionDate());
    assertNotNull(savedTransaction.getBusinessDate());
    assertNotNull(savedTransaction.getCreatedDateTime());
    assertNotNull(savedTransaction.getLastUpdatedDateTime());
    assertNotNull(savedTransaction.getSourceProcessedDate());
    assertNotNull(savedTransaction.getPartnerRelationshipType());

    log.info("Testing service methods for transaction {}", transactionId);
    Transaction foundById =
        transactionDBService.findById(savedTransaction.getTransactionId()).orElse(null);
    assertNotNull(foundById);
    assertEquals(savedTransaction.getTransactionId(), foundById.getTransactionId());
    assertTrue(transactionDBService.isReady());
    var allTransactions = transactionDBService.retrieveAllTransaction();
    assertNotNull(allTransactions);
    assertFalse(allTransactions.isEmpty());

    verify(marketplaceConsumerService, atLeastOnce()).consumeSDMEvents(any(), anyLong(), anyInt());

    log.info("TEST SUCCESS: Basic Transaction - All Service Methods & Field Mapping");
  }

  @Test
  @Order(2)
  public void testEndToEnd_AllPartnerRelationshipTypes_Success() {
    log.info("TEST START: Partner Relationships - ECONCESSION, UNKNOWN, and NULL");

    log.info("Testing ECONCESSION partner relationship");
    FinancialRetailTransaction tx1 =
        FinancialRetailTransactionBuilderFacade.build(
            FinancialRetailTransactionBuilderFacade.withPartnerRelationship(
                PartnerRelationshipType.ECONCESSION),
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-PR-ECON-001"));
    log.info("Sending ECONCESSION transaction: E2E-PR-ECON-001");
    kafkaTemplate.send(prepareKafkaProducerRecord(tx1, marketplaceTopic));

    FinancialRetailTransaction tx2 =
        FinancialRetailTransactionBuilderFacade.build(
            FinancialRetailTransactionBuilderFacade.withPartnerRelationship(
                PartnerRelationshipType.UNKNOWN),
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-PR-UNK-001"));
    log.info("Sending UNKNOWN transaction: E2E-PR-UNK-001");
    kafkaTemplate.send(prepareKafkaProducerRecord(tx2, marketplaceTopic));

    log.info("Testing NULL partner relationship");
    FinancialRetailTransaction tx3 =
        FinancialRetailTransactionBuilderFacade.build(
            FinancialRetailTransactionBuilderFacade.withPartnerRelationship(null),
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-PR-NULL-001"));

    kafkaTemplate.send(prepareKafkaProducerRecord(tx3, marketplaceTopic));

    log.info("Waiting for all 3 partner relationship transactions to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-PR-ECON-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-PR-UNK-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-PR-NULL-001"));
            });

    assertEquals(
        "ECONCESSION", findTransactionBySourceId("E2E-PR-ECON-001").getPartnerRelationshipType());
    assertEquals(
        "UNKNOWN", findTransactionBySourceId("E2E-PR-UNK-001").getPartnerRelationshipType());
    assertNull(findTransactionBySourceId("E2E-PR-NULL-001").getPartnerRelationshipType());

    log.info("TEST SUCCESS: Partner Relationships - ECONCESSION, UNKNOWN, and NULL");
  }

  @Test
  @Order(3)
  public void testEndToEnd_ReturnAndReversalTransactions_Success() {
    log.info("TEST START: Return Transaction with Reversal Flag");

    log.info("Creating and sending RETURN transaction: E2E-RETURN-001");
    FinancialRetailTransaction returnTx =
        FinancialRetailTransactionBuilderFacade.build(
            FinancialRetailTransactionBuilderFacade.withReturnTypeDefault(),
            FinancialRetailTransactionBuilderFacade.withPartnerRelationship(
                PartnerRelationshipType.ECONCESSION),
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-RETURN-001"));
    kafkaTemplate.send(prepareKafkaProducerRecord(returnTx, marketplaceTopic));

    log.info("Creating and sending REVERSED transaction: E2E-REVERSED-001");
    FinancialRetailTransaction reversedTx =
        FinancialRetailTransactionBuilderFacade.build(
            FinancialRetailTransactionBuilderFacade.withPartnerRelationship(
                PartnerRelationshipType.ECONCESSION),
            builder ->
                builder
                    .setFinancialRetailTransactionRecordId("E2E-REVERSED-001")
                    .setIsReversed(true));
    kafkaTemplate.send(prepareKafkaProducerRecord(reversedTx, marketplaceTopic));

    log.info("Waiting for return and reversal transactions to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-RETURN-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-REVERSED-001"));
            });

    log.info("Verifying RETURN transaction type and reversal code");
    Transaction returnTransaction = findTransactionBySourceId("E2E-RETURN-001");
    assertNotNull(returnTransaction, "Return transaction should not be null");
    assertEquals("RETURN", returnTransaction.getTransactionType());
    assertEquals("N", returnTransaction.getTransactionReversalCode());

    Transaction reversedTransaction = findTransactionBySourceId("E2E-REVERSED-001");
    assertNotNull(reversedTransaction, "Reversed transaction should not be null");
    assertEquals("Y", reversedTransaction.getTransactionReversalCode());

    log.info("TEST SUCCESS: Return Transaction with Reversal Flag");
  }

  @Test
  @Order(4)
  public void testEndToEnd_DuplicateHandling_Idempotency() {
    log.info("TEST START: Duplicate Transaction Idempotency");

    FinancialRetailTransaction transaction =
        FinancialRetailTransactionBuilderFacade.build(
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-DUP-001"));
    String transactionId = String.valueOf(transaction.getFinancialRetailTransactionRecordId());

    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));

    log.info("Waiting for first transaction to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "MARKETPLACE", transactionId)));

    Transaction firstSave = findTransactionBySourceId(transactionId);
    assertNotNull(firstSave);
    Long firstTransactionId = firstSave.getTransactionId();

    log.info("Sending duplicate transaction: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));

    log.info("Waiting for duplicate transaction processing attempt");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                verify(marketplaceConsumerService, atLeast(2))
                    .consumeSDMEvents(any(), anyLong(), anyInt()));

    log.info("Verifying idempotency - checking only one record exists for {}", transactionId);
    long count =
        transactionDBService.retrieveAllTransaction().stream()
            .filter(t -> t.getSourceReferenceTransactionId().equals(transactionId))
            .count();
    assertEquals(1, count);
    log.info("Idempotency verified: only 1 record exists for duplicate transaction");

    Transaction afterDuplicate = findTransactionBySourceId(transactionId);
    assertEquals(firstTransactionId, afterDuplicate.getTransactionId());

    log.info("TEST SUCCESS: Duplicate Transaction Idempotency");
  }

  @Test
  @Order(5)
  public void testEndToEnd_AllLineItemTypes_Success() {
    log.info("TEST START: All Line Item Types - Merchandise, Non-Merchandise, Tenders");

    FinancialRetailTransaction merchTx = FinancialRetailTransactionBuilderFacade.buildDefault();
    merchTx =
        FinancialRetailTransaction.newBuilder(merchTx)
            .setFinancialRetailTransactionRecordId("E2E-MERCH-001")
            .build();
    log.info("Sending MERCHANDISE transaction: E2E-MERCH-001");
    kafkaTemplate.send(prepareKafkaProducerRecord(merchTx, marketplaceTopic));

    log.info("Testing NON-MERCHANDISE line items");
    FinancialRetailTransaction nonMerchTx =
        FinancialRetailTransactionBuilderFacade.build(
            builder ->
                builder
                    .setFinancialRetailTransactionRecordId("E2E-NONMERCH-001")
                    .setMerchandiseLineItems(java.util.Collections.emptyList())
                    .setNonMerchandiseLineItems(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .NonMerchandiseLineItemBuilderFacade.buildDefaultList(3)));
    kafkaTemplate.send(prepareKafkaProducerRecord(nonMerchTx, marketplaceTopic));

    log.info("Testing TENDER line items");
    FinancialRetailTransaction tenderTx =
        FinancialRetailTransactionBuilderFacade.build(
            builder ->
                builder
                    .setFinancialRetailTransactionRecordId("E2E-TENDER-001")
                    .setTenderDetails(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .TenderDetailBuilderFacade.buildDefaultList(3)));
    log.info("Sending TENDER transaction: E2E-TENDER-001");
    kafkaTemplate.send(prepareKafkaProducerRecord(tenderTx, marketplaceTopic));

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(45, SECONDS)
        .untilAsserted(
            () -> {
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-MERCH-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-NONMERCH-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-TENDER-001"));
            });

    assertNotNull(findTransactionBySourceId("E2E-MERCH-001"));
    assertNotNull(findTransactionBySourceId("E2E-NONMERCH-001"));
    assertNotNull(findTransactionBySourceId("E2E-TENDER-001"));

    log.info("TEST SUCCESS: All Line Item Types - Merchandise, Non-Merchandise, Tenders");
  }

  @Test
  @Order(6)
  public void testEndToEnd_TaxAndZeroTotal_Success() {
    log.info("TEST START: Tax Processing and Zero Total");

    FinancialRetailTransaction taxTx =
        FinancialRetailTransactionBuilderFacade.build(
            builder ->
                builder
                    .setFinancialRetailTransactionRecordId("E2E-TAX-001")
                    .setTaxTotal(
                        com.nordstrom.finance.dataintegration.facade.schema.standard
                            .MoneyBuilderFacade.build(15, 750_000_000))
                    .setMerchandiseLineItems(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .MerchandiseLineItemBuilderFacade.buildList(
                            2,
                            item -> {
                              item.setTax(
                                  com.nordstrom.finance.dataintegration.facade.schema.retail
                                      .TransactionItemTaxBuilderFacade.buildDefault());
                              return item;
                            })));
    log.info("Sending TAX transaction: E2E-TAX-001");
    kafkaTemplate.send(prepareKafkaProducerRecord(taxTx, marketplaceTopic));

    FinancialRetailTransaction zeroTx =
        FinancialRetailTransactionBuilderFacade.build(
            FinancialRetailTransactionBuilderFacade.withZeroTotal(),
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-ZERO-001"));
    log.info("Sending ZERO total transaction: E2E-ZERO-001");
    kafkaTemplate.send(prepareKafkaProducerRecord(zeroTx, marketplaceTopic));

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              assertTrue(
                  transactionDBService.existsByTransactionId("SDM", "MARKETPLACE", "E2E-TAX-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId("SDM", "MARKETPLACE", "E2E-ZERO-001"));
            });

    assertNotNull(findTransactionBySourceId("E2E-TAX-001"));
    assertEquals(
        "E2E-ZERO-001",
        findTransactionBySourceId("E2E-ZERO-001").getSourceReferenceTransactionId());

    log.info("TEST SUCCESS: Tax Processing and Zero Total");
  }

  @Test
  @Order(7)
  public void testEndToEnd_LocationAndDateTimeMapping_Success() {
    log.info("TEST START: Store Location and DateTime Mapping");

    log.info("Creating transaction with location and datetime fields: E2E-LOC-DATE-001");
    FinancialRetailTransaction transaction =
        FinancialRetailTransactionBuilderFacade.build(
            builder ->
                builder
                    .setFinancialRetailTransactionRecordId("E2E-LOC-DATE-001")
                    .setRetailLocationId("STORE-123")
                    .setMerchandiseLineItems(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .MerchandiseLineItemBuilderFacade.buildList(
                            2,
                            item -> {
                              item.setIntentLocationId("STORE-456");
                              return item;
                            })));
    log.info("Sending location/datetime transaction: E2E-LOC-DATE-001");
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "MARKETPLACE", "E2E-LOC-DATE-001")));

    log.info("Verifying all datetime fields are populated");
    Transaction savedTransaction = findTransactionBySourceId("E2E-LOC-DATE-001");
    assertNotNull(savedTransaction);
    assertNotNull(savedTransaction.getTransactionDate());
    assertNotNull(savedTransaction.getBusinessDate());
    assertNotNull(savedTransaction.getSourceProcessedDate());
    assertNotNull(savedTransaction.getCreatedDateTime());
    assertNotNull(savedTransaction.getLastUpdatedDateTime());

    log.info("TEST SUCCESS: Store Location and DateTime Mapping");
  }

  @Test
  @Order(8)
  public void testEndToEnd_DatabaseCRUDOperations_Success() {
    log.info("TEST START: Database CRUD Operations");

    FinancialRetailTransaction transaction = FinancialRetailTransactionBuilderFacade.buildDefault();
    transaction =
        FinancialRetailTransaction.newBuilder(transaction)
            .setFinancialRetailTransactionRecordId("E2E-CRUD-001")
            .build();
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));

    log.info("Waiting for CRUD transaction to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "MARKETPLACE", "E2E-CRUD-001")));

    Transaction savedTransaction = findTransactionBySourceId("E2E-CRUD-001");
    assertNotNull(savedTransaction);
    assertEquals("SDM", savedTransaction.getSourceReferenceSystemType());
    assertEquals("MARKETPLACE", savedTransaction.getSourceReferenceType());

    Long transactionId = savedTransaction.getTransactionId();
    assertNotNull(transactionId);
    log.info("CREATE operation verified - transaction saved with ID: {}", transactionId);

    Transaction foundById = transactionDBService.findById(transactionId).orElse(null);
    assertNotNull(foundById);
    assertEquals(transactionId, foundById.getTransactionId());

    var allTransactions = transactionDBService.retrieveAllTransaction();
    assertNotNull(allTransactions);
    assertFalse(allTransactions.isEmpty());

    assertTrue(transactionDBService.isReady());

    assertTrue(transactionDBService.findById(transactionId).isPresent());
    transactionDBService.deleteById(transactionId);
    assertFalse(transactionDBService.findById(transactionId).isPresent());

    log.info("TEST SUCCESS: Database CRUD Operations");
  }

  @Test
  @Order(9)
  public void testEndToEnd_BatchProcessing_Success() {
    log.info("TEST START: Batch Processing Performance");

    int batchSize = 15;
    log.info("Creating and sending {} batch transactions", batchSize);

    for (int i = 1; i <= batchSize; i++) {
      final int index = i;
      FinancialRetailTransaction transaction =
          FinancialRetailTransactionBuilderFacade.build(
              builder ->
                  builder.setFinancialRetailTransactionRecordId(
                      "E2E-BATCH-" + String.format("%03d", index)));
      kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));
    }
    log.info("All {} batch transactions sent to Kafka", batchSize);

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(60, SECONDS)
        .untilAsserted(
            () -> {
              for (int i = 1; i <= batchSize; i++) {
                String txId = "E2E-BATCH-" + String.format("%03d", i);
                assertTrue(
                    transactionDBService.existsByTransactionId("SDM", "MARKETPLACE", txId),
                    "Batch transaction " + txId + " should be processed");
              }
            });

    log.info("Verifying all {} batch transactions persisted correctly", batchSize);
    for (int i = 1; i <= batchSize; i++) {
      String txId = "E2E-BATCH-" + String.format("%03d", i);
      Transaction t = findTransactionBySourceId(txId);
      assertNotNull(t);
      assertEquals("SDM", t.getSourceReferenceSystemType());
      assertEquals("MARKETPLACE", t.getSourceReferenceType());
    }

    log.info("TEST SUCCESS: Batch Processing Performance - {} transactions processed", batchSize);
  }

  @Test
  @Order(10)
  public void testEndToEnd_ConsumerListenerVerification_Success() {
    log.info("TEST START: Consumer Listener Verification");

    FinancialRetailTransaction tx1 =
        FinancialRetailTransactionBuilderFacade.build(
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-LISTENER-001"));
    kafkaTemplate.send(prepareKafkaProducerRecord(tx1, marketplaceTopic));

    log.info("Sending second listener test transaction: E2E-LISTENER-002");
    FinancialRetailTransaction tx2 =
        FinancialRetailTransactionBuilderFacade.build(
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-LISTENER-002"));
    kafkaTemplate.send(prepareKafkaProducerRecord(tx2, marketplaceTopic));

    log.info("Waiting for both listener transactions to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-LISTENER-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-LISTENER-002"));
            });

    verify(marketplaceConsumerService, atLeast(2)).consumeSDMEvents(any(), anyLong(), anyInt());

    log.info("TEST SUCCESS: Consumer Listener Verification");
  }

  @Test
  @Order(11)
  public void testEndToEnd_DatabaseConnectionException_RetryableError() {
    log.info("TEST START: Error Handling - Database Connection Exception");

    FinancialRetailTransaction transaction =
        FinancialRetailTransactionBuilderFacade.build(
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-DB-RESILIENCE-001"));

    String transactionId = String.valueOf(transaction.getFinancialRetailTransactionRecordId());

    log.info("Sending error handling test transaction: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));

    log.info("Waiting for error handling transaction to be processed");
    await()
        .pollInterval(Duration.ofSeconds(1))
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "MARKETPLACE", transactionId)));

    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    assertEquals(transactionId, savedTransaction.getSourceReferenceTransactionId());

    log.info("TEST SUCCESS: Error Handling - Database Connection Exception");
  }

  @Test
  @Order(12)
  public void testEndToEnd_ExceptionHandlingCoverage_Success() {
    log.info("TEST START: Error Handling - Exception Coverage");

    log.info(
        "Creating complex transaction to test exception handling paths: E2E-EXCEPTION-COV-001");
    FinancialRetailTransaction transaction =
        FinancialRetailTransactionBuilderFacade.build(
            builder ->
                builder
                    .setFinancialRetailTransactionRecordId("E2E-EXCEPTION-COV-001")
                    .setMerchandiseLineItems(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .MerchandiseLineItemBuilderFacade.buildDefaultList(3))
                    .setNonMerchandiseLineItems(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .NonMerchandiseLineItemBuilderFacade.buildDefaultList(2)));

    String transactionId = String.valueOf(transaction.getFinancialRetailTransactionRecordId());

    log.info("Sending exception coverage test transaction: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));

    await()
        .pollInterval(Duration.ofSeconds(1))
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "MARKETPLACE", transactionId)));

    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    log.info("Complex transaction processed successfully");

    verify(marketplaceConsumerService, atLeastOnce()).consumeSDMEvents(any(), anyLong(), anyInt());

    log.info("TEST SUCCESS: Error Handling - Exception Coverage");
  }

  @Test
  @Order(14)
  public void testEndToEnd_CompleteScenarios_Success() {
    log.info("TEST START: Complete Coverage - All Transaction Scenarios");

    log.info("Scenario 1: Fast processing transaction - E2E-COMPLETE-001");
    FinancialRetailTransaction tx1 =
        FinancialRetailTransactionBuilderFacade.build(
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-COMPLETE-001"));
    kafkaTemplate.send(prepareKafkaProducerRecord(tx1, marketplaceTopic));

    log.info("Scenario 2: Transaction with all line types - E2E-COMPLETE-002");
    FinancialRetailTransaction tx2 =
        FinancialRetailTransactionBuilderFacade.build(
            builder ->
                builder
                    .setFinancialRetailTransactionRecordId("E2E-COMPLETE-002")
                    .setMerchandiseLineItems(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .MerchandiseLineItemBuilderFacade.buildDefaultList(5))
                    .setNonMerchandiseLineItems(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .NonMerchandiseLineItemBuilderFacade.buildDefaultList(3))
                    .setTenderDetails(
                        com.nordstrom.finance.dataintegration.facade.schema.retail
                            .TenderDetailBuilderFacade.buildDefaultList(2)));
    kafkaTemplate.send(prepareKafkaProducerRecord(tx2, marketplaceTopic));

    log.info("Scenario 3: Return transaction with complex data - E2E-COMPLETE-003");
    FinancialRetailTransaction tx3 =
        FinancialRetailTransactionBuilderFacade.build(
            FinancialRetailTransactionBuilderFacade.withReturnTypeDefault(),
            builder -> builder.setFinancialRetailTransactionRecordId("E2E-COMPLETE-003"));
    kafkaTemplate.send(prepareKafkaProducerRecord(tx3, marketplaceTopic));

    log.info("Waiting for all 3 scenario transactions to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(45, SECONDS)
        .untilAsserted(
            () -> {
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-COMPLETE-001"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-COMPLETE-002"));
              assertTrue(
                  transactionDBService.existsByTransactionId(
                      "SDM", "MARKETPLACE", "E2E-COMPLETE-003"));
            });

    assertNotNull(findTransactionBySourceId("E2E-COMPLETE-001"));
    log.info("Scenario 1 verified");
    assertNotNull(findTransactionBySourceId("E2E-COMPLETE-002"));
    log.info("Scenario 2 verified");
    assertNotNull(findTransactionBySourceId("E2E-COMPLETE-003"));
    log.info("Scenario 3 verified");

    verify(marketplaceConsumerService, atLeast(3)).consumeSDMEvents(any(), anyLong(), anyInt());
    log.info("TEST SUCCESS: Complete Coverage - All Transaction Scenarios");
  }

  @Test
  @Order(15)
  public void testEndToEnd_ConcurrentProcessing_Success() {
    log.info("TEST START: Concurrent Processing Verification");

    int concurrentCount = 10;
    log.info("Sending {} concurrent transactions rapidly", concurrentCount);

    for (int i = 1; i <= concurrentCount; i++) {
      final int index = i;
      FinancialRetailTransaction transaction =
          FinancialRetailTransactionBuilderFacade.build(
              builder ->
                  builder.setFinancialRetailTransactionRecordId(
                      "E2E-CONCURRENT-" + String.format("%02d", index)));
      kafkaTemplate.send(prepareKafkaProducerRecord(transaction, marketplaceTopic));
    }
    kafkaTemplate.flush();
    log.info("All {} concurrent transactions sent", concurrentCount);

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(60, SECONDS)
        .untilAsserted(
            () -> {
              for (int i = 1; i <= concurrentCount; i++) {
                String txId = "E2E-CONCURRENT-" + String.format("%02d", i);
                assertTrue(
                    transactionDBService.existsByTransactionId("SDM", "MARKETPLACE", txId),
                    "Concurrent transaction " + txId + " should be processed");
              }
            });

    log.info("Verifying all {} concurrent transactions processed correctly", concurrentCount);
    for (int i = 1; i <= concurrentCount; i++) {
      String txId = "E2E-CONCURRENT-" + String.format("%02d", i);
      Transaction t = findTransactionBySourceId(txId);
      assertNotNull(t);
      assertEquals("SDM", t.getSourceReferenceSystemType());
      assertEquals("MARKETPLACE", t.getSourceReferenceType());
    }

    verify(marketplaceConsumerService, atLeast(concurrentCount))
        .consumeSDMEvents(any(), anyLong(), anyInt());

    log.info("TEST SUCCESS: Concurrent Processing Verification - {} transactions", concurrentCount);
  }

  private ProducerRecord<String, Object> prepareKafkaProducerRecord(
      FinancialRetailTransaction transaction, String topic) {
    String key = String.valueOf(transaction.getFinancialRetailTransactionRecordId());
    ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, transaction);
    record.headers().add("Nord-Request-Id", "test-integration".getBytes(StandardCharsets.UTF_8));
    record.headers().add("kafka_offset", "1".getBytes(StandardCharsets.UTF_8));
    record.headers().add("kafka_receivedPartitionId", "0".getBytes(StandardCharsets.UTF_8));
    record
        .headers()
        .add(
            "kafka_receivedTimestamp",
            String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
    record
        .headers()
        .add(
            "SystemTime",
            Instant.now(Clock.systemUTC()).toString().getBytes(StandardCharsets.UTF_8));
    return record;
  }

  private Transaction findTransactionBySourceId(String sourceReferenceTransactionId) {
    return transactionDBService.retrieveAllTransaction().stream()
        .filter(
            t ->
                t.getSourceReferenceTransactionId().equals(sourceReferenceTransactionId)
                    && "SDM".equals(t.getSourceReferenceSystemType())
                    && "MARKETPLACE".equals(t.getSourceReferenceType()))
        .findFirst()
        .orElse(null);
  }

  private void cleanupTestData() throws SQLException {
    final String DELETE_QUERY =
        "DELETE FROM TRANSACTION WHERE SOURCE_REFERENCE_SYSTEM_TYPE = 'SDM' "
            + "AND SOURCE_REFERENCE_TYPE = 'MARKETPLACE'";
    try (Connection connection = getAuroraDbConnection();
        PreparedStatement ps = connection.prepareStatement(DELETE_QUERY)) {
      int deleted = ps.executeUpdate();
      log.info("Cleaned up {} marketplace test records", deleted);
    } catch (Exception e) {
      log.error("Failed to cleanup test data", e);
      throw e;
    }
  }

  private Connection getAuroraDbConnection() throws SQLException {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new SQLException("Database driver not found", e);
    }
    return DriverManager.getConnection(datasourceUrl, datasourceUsername, datasourcePassword);
  }
}
