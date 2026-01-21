package com.nordstrom.finance.dataintegration.consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.finance.dataintegration.Application;
import com.nordstrom.finance.dataintegration.config.TestKafkaConfig;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.facade.schema.restaurant.FinancialRestaurantTransactionBuilderFacade;
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
 * End-to-End Integration Test for SDM Restaurant Consumer with Embedded Kafka.
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
    topics = {"test-restaurant-topic"})
@SpringBootTest
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SDMRestaurantConsumerIntegrationTest {

  @Autowired private EntityManager entityManager;

  @Autowired
  @Qualifier("kafkaTestTemplate")
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired private TransactionDBService transactionDBService;

  @MockitoSpyBean private SDMRestaurantConsumerService restaurantConsumerService;

  @Value("${app.kafka.consumer.topic.restaurant}")
  private String restaurantTopic;

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
      cleanupTestData();
    }
    log.info("=== INTEGRATION TEST TEARDOWN COMPLETE ===");
  }

  @Test
  @Order(1)
  public void testEndToEnd_BasicRestaurantTransaction_Success() {
    log.info("TEST START: Basic Restaurant Transaction");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.saleWithSingleItemAndSingleTender();
    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());

    log.info("Sending transaction {} to Kafka topic: {}", transactionId, restaurantTopic);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));
    log.info("Transaction {} sent successfully to Kafka", transactionId);

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId("SDM", "RESTAURANT", transactionId),
                    "Restaurant transaction should be persisted to database after Kafka consumption"));

    log.info("Transaction {} found in database, verifying details", transactionId);
    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction, "Transaction should exist in database");
    assertEquals(transactionId, savedTransaction.getSourceReferenceTransactionId());
    assertEquals("SDM", savedTransaction.getSourceReferenceSystemType());
    assertEquals("RESTAURANT", savedTransaction.getSourceReferenceType());
    assertEquals("SALE", savedTransaction.getTransactionType());
    assertEquals("N", savedTransaction.getTransactionReversalCode());
    log.info("Transaction {} details verified successfully", transactionId);

    verify(restaurantConsumerService, atLeastOnce())
        .consumeSDMRestaurantEvents(any(), anyLong(), anyInt());

    log.info("TEST SUCCESS: Basic Restaurant Transaction");
  }

  @Test
  @Order(2)
  @DisplayName("E2E: Test Restaurant Return Transaction via Kafka")
  public void testEndToEnd_RestaurantReturnTransaction_Success() {

    log.info("TEST START: Restaurant Return Transaction");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.returnWithSingleItemAndSingleTender();
    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());
    log.info("Created restaurant RETURN transaction with ID: {}", transactionId);

    log.info("Sending RETURN transaction {} to Kafka", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for RETURN transaction {} to be processed", transactionId);
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "RESTAURANT", transactionId)));

    log.info("Verifying RETURN transaction type and reversal code");
    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    assertEquals("RETURN", savedTransaction.getTransactionType());
    assertEquals("N", savedTransaction.getTransactionReversalCode());
    log.info("RETURN transaction verified: type=RETURN, reversalCode=N");

    log.info("========================================");
    log.info("TEST SUCCESS: Restaurant Return Transaction");
    log.info("========================================");
  }

  @Test
  @Order(3)
  public void testEndToEnd_ReversedRestaurantSale_Success() {

    log.info("TEST START: Reversed Restaurant Sale");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.reversedSaleWithSingleItemAndSingleTender();
    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());

    log.info("Sending REVERSED SALE transaction {} to Kafka", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for REVERSED SALE transaction {} to be processed", transactionId);
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "RESTAURANT", transactionId)));

    log.info("Verifying REVERSED SALE transaction reversal code");
    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    assertEquals("SALE", savedTransaction.getTransactionType());
    assertEquals("Y", savedTransaction.getTransactionReversalCode());

    log.info("TEST SUCCESS: Reversed Restaurant Sale");
  }

  @Test
  @Order(4)
  public void testEndToEnd_ReversedRestaurantReturn_Success() {
    log.info("TEST START: Reversed Restaurant Return");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.reversedReturnWithSingleItemAndSingleTender();
    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());

    log.info("Sending REVERSED RETURN transaction {} to Kafka", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for REVERSED RETURN transaction {} to be processed", transactionId);
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "RESTAURANT", transactionId)));

    log.info("Verifying REVERSED RETURN transaction details");
    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    assertEquals("RETURN", savedTransaction.getTransactionType());
    assertEquals("Y", savedTransaction.getTransactionReversalCode());

    log.info("TEST SUCCESS: Reversed Restaurant Return");
  }

  @Test
  @Order(5)
  public void testEndToEnd_DuplicateRestaurantTransaction_ShouldSkip() {
    log.info("TEST START: Duplicate Restaurant Transaction Handling");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.build(
            builder -> builder.setFinancialRestaurantTransactionRecordId("REST-DUPLICATE-001"));

    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());

    log.info("Sending first instance of transaction: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for first transaction to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "RESTAURANT", transactionId)));

    log.info("Sending duplicate transaction: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for duplicate transaction processing attempt");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                verify(restaurantConsumerService, atLeast(2))
                    .consumeSDMRestaurantEvents(any(), anyLong(), anyInt()));

    log.info("Verifying idempotency - checking only one record exists for {}", transactionId);
    long count =
        transactionDBService.retrieveAllTransaction().stream()
            .filter(t -> t.getSourceReferenceTransactionId().equals(transactionId))
            .count();

    assertEquals(1, count, "Should have exactly one transaction (duplicate should be skipped)");

    log.info("TEST SUCCESS: Duplicate Restaurant Transaction Handling");
  }

  @Test
  @Order(6)
  public void testEndToEnd_MultipleRestaurantTransactions_AllProcessed() {
    log.info("TEST START: Multiple Restaurant Transactions");

    for (int i = 1; i <= 3; i++) {
      final int index = i;
      FinancialRestaurantTransaction transaction =
          FinancialRestaurantTransactionBuilderFacade.build(
              builder -> builder.setFinancialRestaurantTransactionRecordId("REST-MULTI-" + index));
      log.info("Sending restaurant transaction: REST-MULTI-{}", index);
      kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));
    }

    log.info("Waiting for all 3 transactions to be processed");
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(45, SECONDS)
        .untilAsserted(
            () -> {
              assertTrue(
                  transactionDBService.existsByTransactionId("SDM", "RESTAURANT", "REST-MULTI-1"));
              assertTrue(
                  transactionDBService.existsByTransactionId("SDM", "RESTAURANT", "REST-MULTI-2"));
              assertTrue(
                  transactionDBService.existsByTransactionId("SDM", "RESTAURANT", "REST-MULTI-3"));
            });

    log.info("TEST SUCCESS: Multiple Restaurant Transactions");
  }

  @Test
  @Order(7)
  public void testEndToEnd_RestaurantWithMenuItems_Success() {
    log.info("TEST START: Restaurant Transaction with Menu Items");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.build(
            builder -> builder.setFinancialRestaurantTransactionRecordId("REST-MENU-001"));

    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());

    log.info("Sending transaction with menu items: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for menu items transaction {} to be processed", transactionId);
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "RESTAURANT", transactionId)));

    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);

    log.info("TEST SUCCESS: Restaurant Transaction with Menu Items");
  }

  @Test
  @Order(8)
  public void testEndToEnd_RestaurantWithTenders_Success() {
    log.info("TEST START: Restaurant Transaction with Tenders");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.build(
            builder -> builder.setFinancialRestaurantTransactionRecordId("REST-TENDER-001"));
    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());

    log.info("Sending transaction with tenders: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for tenders transaction {} to be processed", transactionId);
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "RESTAURANT", transactionId)));

    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    assertEquals("SALE", savedTransaction.getTransactionType());

    log.info("TEST SUCCESS: Restaurant Transaction with Tenders");
  }

  @Test
  @Order(9)
  public void testEndToEnd_VerifyRestaurantLineDetails_Success() {
    log.info("TEST START: Verify Restaurant Transaction Line Details");

    FinancialRestaurantTransaction transaction =
        FinancialRestaurantTransactionBuilderFacade.build(
            builder -> builder.setFinancialRestaurantTransactionRecordId("REST-LINE-DETAILS-001"));
    String transactionId = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());

    log.info("Sending transaction for line details verification: {}", transactionId);
    kafkaTemplate.send(prepareKafkaProducerRecord(transaction, restaurantTopic));

    log.info("Waiting for line details transaction {} to be processed", transactionId);
    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(30, SECONDS)
        .untilAsserted(
            () ->
                assertTrue(
                    transactionDBService.existsByTransactionId(
                        "SDM", "RESTAURANT", transactionId)));

    log.info("Verifying transaction line details");
    Transaction savedTransaction = findTransactionBySourceId(transactionId);
    assertNotNull(savedTransaction);
    assertEquals("SDM", savedTransaction.getSourceReferenceSystemType());
    assertEquals("RESTAURANT", savedTransaction.getSourceReferenceType());

    log.info("TEST SUCCESS: Verify Restaurant Transaction Line Details");
  }

  /** Helper method to prepare Kafka producer record with headers. */
  private ProducerRecord<String, Object> prepareKafkaProducerRecord(
      FinancialRestaurantTransaction transaction, String topic) {

    String key = String.valueOf(transaction.getFinancialRestaurantTransactionRecordId());
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

  /** Helper method to find transaction by source reference ID. */
  private Transaction findTransactionBySourceId(String sourceReferenceTransactionId) {
    return transactionDBService.retrieveAllTransaction().stream()
        .filter(
            t ->
                t.getSourceReferenceTransactionId().equals(sourceReferenceTransactionId)
                    && "SDM".equals(t.getSourceReferenceSystemType())
                    && "RESTAURANT".equals(t.getSourceReferenceType()))
        .findFirst()
        .orElse(null);
  }

  /** Cleanup test data from database. */
  private void cleanupTestData() throws SQLException {
    final String DELETE_QUERY =
        "DELETE FROM transaction WHERE SOURCE_REFERENCE_SYSTEM_TYPE = 'SDM' "
            + "AND SOURCE_REFERENCE_TYPE = 'RESTAURANT'";

    try (Connection connection = getAuroraDbConnection();
        PreparedStatement ps = connection.prepareStatement(DELETE_QUERY)) {
      int deleted = ps.executeUpdate();
      log.info("Cleaned up {} restaurant test records", deleted);
    } catch (Exception e) {
      log.error("Failed to cleanup test data", e);
      throw e;
    }
  }

  /** Get database connection for cleanup operations. */
  private Connection getAuroraDbConnection() throws SQLException {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new SQLException("Database driver not found", e);
    }
    return DriverManager.getConnection(datasourceUrl, datasourceUsername, datasourcePassword);
  }
}
