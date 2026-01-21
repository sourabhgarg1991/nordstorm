package com.nordstrom.finance.dataintegration.transactionaggregator.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nordstrom.finance.dataintegration.transactionaggregator.config.IntegrationTestRepositoryConfig;
import com.nordstrom.finance.dataintegration.transactionaggregator.config.TestMetricsConfiguration;
import com.nordstrom.finance.dataintegration.transactionaggregator.config.TestS3Configuration;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.AggregationConfigurationRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.GeneratedFileDetailRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.TransactionAggregationRelationRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.service.AggregationCommandLineRunnerService;
import com.nordstrom.finance.dataintegration.transactionaggregator.util.AggregationAssertionHelper;
import com.nordstrom.finance.dataintegration.transactionaggregator.util.AggregationTestDataBuilder;
import com.nordstrom.finance.dataintegration.transactionaggregator.util.DatabaseCleanupHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for AggregationCommandLineRunnerService. These tests verify end-to-end
 * aggregation processing against a real Aurora test database.
 *
 * <p>Test Scenarios: 1. Individual configuration tests (Retail, Restaurant, Marketplace, Promotion)
 * 2. Empty result handling 3. Multiple configurations (hybrid scenario)
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Import({
  TestMetricsConfiguration.class,
  TestS3Configuration.class,
  DatabaseCleanupHelper.class,
  IntegrationTestRepositoryConfig.class
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregationCommandLineRunnerIntegrationTest {

  @Autowired private AggregationCommandLineRunnerService aggregationService;
  @Autowired private AggregationConfigurationRepository configurationRepository;
  @Autowired private GeneratedFileDetailRepository fileDetailRepository;
  @Autowired private TransactionAggregationRelationRepository relationRepository;
  @Autowired private DatabaseCleanupHelper cleanupHelper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeAll
  void setUpOnce() {
    log.info("========== Starting Integration Tests ==========");
    log.info("Test configuration beans loaded successfully");
  }

  @BeforeEach
  void setUp() {
    log.info("Cleaning database before test...");
    cleanupHelper.cleanAllTables();
  }

  @AfterAll
  void tearDownOnce() {
    log.info("========== Integration Tests Completed ==========");
    cleanupHelper.cleanAllTables();
  }

  // ==================== TEST CASE 1: Retail Aggregation ====================

  @Test
  @Order(1)
  @DisplayName(
      "Test 1: Retail Transaction Aggregation - Should aggregate retail transactions successfully")
  void testRetailAggregationConfiguration_Success() {
    log.info("========== TEST 1: Retail Aggregation ==========");

    createTestTransaction("ERTM", "RETAIL-001", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");

    createTestTransactionLine(transactionId, "RETAIL-LINE-001", "SALE", "0001");
    Long lineId = getLastInsertedId("transaction_line");

    createTestRetailTransactionLine(
        lineId, "100.00", "10.00", "5.00", "115.00", "DEPT01", "CLASS01");

    var config = AggregationTestDataBuilder.buildRetailAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    var relations = relationRepository.findAll();

    log.info("Generated {} file(s) and {} relation(s)", fileDetails.size(), relations.size());

    AggregationAssertionHelper.assertAggregationSuccess(fileDetails, relations, 1, 1);
    AggregationAssertionHelper.assertGeneratedFileDetail(
        fileDetails.get(0),
        "Retail_Transaction_Aggregation",
        '|',
        config.getAggregationConfigurationId());
    AggregationAssertionHelper.assertCSVFileContent(
        fileDetails.get(0).getFileContent(), '|', false, 2);
    AggregationAssertionHelper.assertAggregationRelations(relations, 1);

    log.info("Test 1 PASSED: Retail aggregation successful");
  }

  // ==================== TEST CASE 2: Restaurant Aggregation ====================

  @Test
  @Order(2)
  @DisplayName(
      "Test 2: Restaurant Transaction Aggregation - Should aggregate restaurant transactions")
  void testRestaurantAggregationConfiguration_Success() {
    log.info("========== TEST 2: Restaurant Aggregation ==========");

    createTestTransaction("ERTM", "RESTAURANT-001", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");

    createTestTransactionLine(transactionId, "REST-LINE-001", "SALE", "0002");
    Long lineId = getLastInsertedId("transaction_line");

    createTestRestaurantTransactionLine(
        lineId, "75.00", "5.00", "3.75", "83.75", "15.00", "DEPT02");

    var config = AggregationTestDataBuilder.buildRestaurantAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    var relations = relationRepository.findAll();

    log.info("Generated {} file(s) and {} relation(s)", fileDetails.size(), relations.size());

    AggregationAssertionHelper.assertAggregationSuccess(fileDetails, relations, 1, 1);
    AggregationAssertionHelper.assertGeneratedFileDetail(
        fileDetails.get(0),
        "Restaurant_Transaction_Aggregation",
        '|',
        config.getAggregationConfigurationId());
    AggregationAssertionHelper.assertAggregationRelations(relations, 1);

    log.info("Test 2 PASSED: Restaurant aggregation successful");
  }

  // ==================== TEST CASE 3: Marketplace Aggregation ====================

  @Test
  @Order(3)
  @DisplayName(
      "Test 3: Marketplace Transaction Aggregation - Should aggregate marketplace transactions")
  void testMarketplaceAggregationConfiguration_Success() {
    log.info("========== TEST 3: Marketplace Aggregation ==========");

    createTestTransaction("ERTM", "MARKETPLACE-001", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");

    createTestTransactionLine(transactionId, "MKT-LINE-001", "SALE", "0003");
    Long lineId = getLastInsertedId("transaction_line");

    createTestMarketplaceTransactionLine(lineId, "200.00", "10.00", "210.00", "20.00");

    var config = AggregationTestDataBuilder.buildMarketplaceAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    var relations = relationRepository.findAll();

    log.info("Generated {} file(s) and {} relation(s)", fileDetails.size(), relations.size());

    AggregationAssertionHelper.assertAggregationSuccess(fileDetails, relations, 1, 1);
    AggregationAssertionHelper.assertGeneratedFileDetail(
        fileDetails.get(0),
        "Marketplace_Transaction_Aggregation",
        '|',
        config.getAggregationConfigurationId());
    AggregationAssertionHelper.assertAggregationRelations(relations, 1);

    log.info("Test 3 PASSED: Marketplace aggregation successful");
  }

  // ==================== TEST CASE 4: Promotion Aggregation ====================

  @Test
  @Order(4)
  @DisplayName(
      "Test 4: Promotion Transaction Aggregation - Should aggregate promotion transactions")
  void testPromotionAggregationConfiguration_Success() {
    log.info("========== TEST 4: Promotion Aggregation ==========");

    createTestTransaction("PROMO", "PROMO-001", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");

    createTestTransactionLine(transactionId, "PROMO-LINE-001", "DISCOUNT", "0004");
    Long lineId = getLastInsertedId("transaction_line");

    createTestPromotionTransactionLine(lineId, "-25.00", "LOYALTY_PROMO");

    var config = AggregationTestDataBuilder.buildPromotionAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    var relations = relationRepository.findAll();

    log.info("Generated {} file(s) and {} relation(s)", fileDetails.size(), relations.size());

    AggregationAssertionHelper.assertAggregationSuccess(fileDetails, relations, 1, 1);
    AggregationAssertionHelper.assertGeneratedFileDetail(
        fileDetails.get(0),
        "Promotion_Transaction_Aggregation",
        '|',
        config.getAggregationConfigurationId());
    AggregationAssertionHelper.assertAggregationRelations(relations, 1);

    log.info("Test 4 PASSED: Promotion aggregation successful");
  }

  // ==================== TEST CASE 5: Empty Result ====================

  @Test
  @Order(5)
  @DisplayName("Test 5: Empty Result Handling - Should handle configurations with no data")
  void testEmptyResultConfiguration_NoFileGenerated() {
    log.info("========== TEST 5: Empty Result Handling ==========");

    var config = AggregationTestDataBuilder.buildEmptyResultConfig();
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    var relations = relationRepository.findAll();

    log.info("Generated {} file(s) and {} relation(s)", fileDetails.size(), relations.size());

    AggregationAssertionHelper.assertNoAggregationResults(fileDetails, relations);

    log.info("Test 5 PASSED: Empty result handled correctly (no files generated)");
  }

  // ==================== TEST CASE 6: Hybrid - Multiple Configurations ====================

  @Test
  @Order(6)
  @DisplayName(
      "Test 6: Multiple Configurations (Hybrid) - Should process all active configurations")
  void testMultipleConfigurations_HybridScenario() {
    log.info("========== TEST 6: Multiple Configurations (Hybrid) ==========");

    // Retail transaction
    createTestTransaction("ERTM", "RETAIL-002", LocalDate.now().minusDays(1));
    Long retailTxnId = getLastInsertedId("transaction");
    createTestTransactionLine(retailTxnId, "RETAIL-LINE-002", "SALE", "0001");
    Long retailLineId = getLastInsertedId("transaction_line");
    createTestRetailTransactionLine(
        retailLineId, "50.00", "5.00", "2.50", "57.50", "DEPT01", "CLASS01");

    // Promotion transaction
    createTestTransaction("PROMO", "PROMO-002", LocalDate.now().minusDays(1));
    Long promoTxnId = getLastInsertedId("transaction");
    createTestTransactionLine(promoTxnId, "PROMO-LINE-002", "DISCOUNT", "0004");
    Long promoLineId = getLastInsertedId("transaction_line");
    createTestPromotionTransactionLine(promoLineId, "-10.00", "MARKETING_PROMO");

    // Save multiple configurations
    var retailConfig = AggregationTestDataBuilder.buildRetailAggregationConfig(false);
    var promoConfig = AggregationTestDataBuilder.buildPromotionAggregationConfig(false);
    configurationRepository.save(retailConfig);
    configurationRepository.save(promoConfig);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    var relations = relationRepository.findAll();

    log.info("Generated {} file(s) and {} relation(s)", fileDetails.size(), relations.size());

    AggregationAssertionHelper.assertAggregationSuccess(fileDetails, relations, 2, 2);

    // Verify retail file was generated
    var retailFile =
        fileDetails.stream()
            .filter(f -> f.getGeneratedFileName().startsWith("Retail_Transaction_Aggregation"))
            .findFirst();
    assertThat(retailFile).as("Retail file should be generated").isPresent();
    AggregationAssertionHelper.assertGeneratedFileDetail(
        retailFile.get(),
        "Retail_Transaction_Aggregation",
        '|',
        retailConfig.getAggregationConfigurationId());

    // Verify promotion file was generated
    var promoFile =
        fileDetails.stream()
            .filter(f -> f.getGeneratedFileName().startsWith("Promotion_Transaction_Aggregation"))
            .findFirst();
    assertThat(promoFile).as("Promotion file should be generated").isPresent();
    AggregationAssertionHelper.assertGeneratedFileDetail(
        promoFile.get(),
        "Promotion_Transaction_Aggregation",
        '|',
        promoConfig.getAggregationConfigurationId());

    // Verify relations contain both transaction lines
    var retailRelations =
        relations.stream().filter(r -> r.getTransactionLineId().equals(retailLineId)).toList();
    var promoRelations =
        relations.stream().filter(r -> r.getTransactionLineId().equals(promoLineId)).toList();

    assertThat(retailRelations).as("Retail relation should exist").hasSize(1);
    assertThat(promoRelations).as("Promotion relation should exist").hasSize(1);

    log.info("Test 6 PASSED: Multiple configurations processed successfully");
  }

  @Test
  @Order(7)
  @DisplayName(
      "Test 7: Retail Aggregation - Non-retail amount fields should default to '0' when NULL")
  void testRetailAggregation_CrossTypeAmountFieldsAreZero() {
    log.info(
        "========== TEST 7: Retail Aggregation - Cross-type amounts default to '0' when NULL ==========");

    createTestTransaction("ERTM", "RETAIL-003", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");
    createTestTransactionLine(transactionId, "RETAIL-LINE-003", "SALE", "0001");
    Long lineId = getLastInsertedId("transaction_line");
    createTestRetailTransactionLine(
        lineId, "100.00", "10.00", "5.00", "115.00", "DEPT01", "CLASS01");

    var config = AggregationTestDataBuilder.buildRetailAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    assertThat(fileDetails).isNotEmpty();
    String csvContent = fileDetails.get(0).getFileContent();

    String[] lines = csvContent.split("\n");
    assertThat(lines.length).isGreaterThanOrEqualTo(2);

    String headerLine = lines[0];
    String dataLine = lines[1];

    assertThat(headerLine).contains("MARKETPLACE_JWN_COMMISSION_AMOUNT");
    assertThat(headerLine).contains("RESTAURANT_TIP_AMOUNT");
    assertThat(headerLine).contains("PROMO_AMOUNT");

    assertThat(dataLine).contains("100.00");
    assertThat(dataLine).contains("10.00");
    assertThat(dataLine).contains("5.00");
    assertThat(dataLine).contains("115.00");

    String[] values = dataLine.split("\\|", -1);
    String[] headers = headerLine.split("\\|", -1);

    int marketplaceIdx = findHeaderIndex(headers, "MARKETPLACE_JWN_COMMISSION_AMOUNT");
    int restaurantTipIdx = findHeaderIndex(headers, "RESTAURANT_TIP_AMOUNT");
    int promoIdx = findHeaderIndex(headers, "PROMO_AMOUNT");

    assertThat(values[marketplaceIdx].trim()).isEqualTo("0");
    assertThat(values[restaurantTipIdx].trim()).isEqualTo("0");
    assertThat(values[promoIdx].trim()).isEqualTo("0");

    log.info("Test 7 PASSED: Retail aggregation sets cross-type amount fields to '0' (not NULL)");
  }

  @Test
  @Order(8)
  @DisplayName(
      "Test 8: Restaurant Aggregation - Non-restaurant amount fields should default to '0' when NULL")
  void testRestaurantAggregation_CrossTypeAmountFieldsAreZero() {
    log.info(
        "========== TEST 8: Restaurant Aggregation - Cross-type amounts default to '0' when NULL ==========");

    createTestTransaction("ERTM", "RESTAURANT-003", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");
    createTestTransactionLine(transactionId, "REST-LINE-003", "SALE", "0002");
    Long lineId = getLastInsertedId("transaction_line");
    createTestRestaurantTransactionLine(
        lineId, "75.00", "5.00", "3.75", "83.75", "15.00", "DEPT02");

    var config = AggregationTestDataBuilder.buildRestaurantAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    assertThat(fileDetails).isNotEmpty();
    String csvContent = fileDetails.get(0).getFileContent();

    String[] lines = csvContent.split("\n");
    assertThat(lines.length).isGreaterThanOrEqualTo(2);

    String headerLine = lines[0];
    String dataLine = lines[1];

    assertThat(headerLine).contains("MARKETPLACE_JWN_COMMISSION_AMOUNT");
    assertThat(headerLine).contains("WAIVED_AMOUNT");
    assertThat(headerLine).contains("PROMO_AMOUNT");

    assertThat(dataLine).contains("75.00");
    assertThat(dataLine).contains("15.00");
    assertThat(dataLine).contains("3.75");
    assertThat(dataLine).contains("83.75");

    // Verify cross-type amount fields are '0' (not empty/NULL)
    String[] values = dataLine.split("\\|", -1);
    String[] headers = headerLine.split("\\|", -1);

    int marketplaceIdx = findHeaderIndex(headers, "MARKETPLACE_JWN_COMMISSION_AMOUNT");
    int waivedIdx = findHeaderIndex(headers, "WAIVED_AMOUNT");
    int promoIdx = findHeaderIndex(headers, "PROMO_AMOUNT");

    assertThat(values[marketplaceIdx].trim()).isEqualTo("0");
    assertThat(values[waivedIdx].trim()).isEqualTo("0");
    assertThat(values[promoIdx].trim()).isEqualTo("0");

    log.info(
        "Test 8 PASSED: Restaurant aggregation sets cross-type amount fields to '0' (not NULL)");
  }

  @Test
  @Order(9)
  @DisplayName(
      "Test 9: Marketplace Aggregation - Non-marketplace amount fields should default to '0' when NULL")
  void testMarketplaceAggregation_CrossTypeAmountFieldsAreZero() {
    log.info(
        "========== TEST 9: Marketplace Aggregation - Cross-type amounts default to '0' when NULL ==========");

    createTestTransaction("ERTM", "MARKETPLACE-003", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");
    createTestTransactionLine(transactionId, "MKT-LINE-003", "SALE", "0003");
    Long lineId = getLastInsertedId("transaction_line");
    createTestMarketplaceTransactionLine(lineId, "200.00", "10.00", "210.00", "20.00");

    var config = AggregationTestDataBuilder.buildMarketplaceAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    assertThat(fileDetails).isNotEmpty();
    String csvContent = fileDetails.get(0).getFileContent();

    String[] lines = csvContent.split("\n");
    assertThat(lines.length).isGreaterThanOrEqualTo(2);

    String headerLine = lines[0];
    String dataLine = lines[1];

    assertThat(headerLine).contains("EMPLOYEE_DISCOUNT_AMOUNT");
    assertThat(headerLine).contains("WAIVED_AMOUNT");
    assertThat(headerLine).contains("RESTAURANT_TIP_AMOUNT");
    assertThat(headerLine).contains("PROMO_AMOUNT");

    assertThat(dataLine).contains("200.00");
    assertThat(dataLine).contains("10.00");
    assertThat(dataLine).contains("210.00");
    assertThat(dataLine).contains("20.00");

    // Verify cross-type amount fields are '0' (not empty/NULL)
    String[] values = dataLine.split("\\|", -1);
    String[] headers = headerLine.split("\\|", -1);

    int employeeDiscountIdx = findHeaderIndex(headers, "EMPLOYEE_DISCOUNT_AMOUNT");
    int waivedIdx = findHeaderIndex(headers, "WAIVED_AMOUNT");
    int restaurantTipIdx = findHeaderIndex(headers, "RESTAURANT_TIP_AMOUNT");
    int promoIdx = findHeaderIndex(headers, "PROMO_AMOUNT");

    assertThat(values[employeeDiscountIdx].trim()).isEqualTo("0");
    assertThat(values[waivedIdx].trim()).isEqualTo("0");
    assertThat(values[restaurantTipIdx].trim()).isEqualTo("0");
    assertThat(values[promoIdx].trim()).isEqualTo("0");

    log.info(
        "Test 9 PASSED: Marketplace aggregation sets cross-type amount fields to '0' (not NULL)");
  }

  @Test
  @Order(10)
  @DisplayName(
      "Test 10: Promotion Aggregation - All non-promotion amount fields should default to '0' when NULL")
  void testPromotionAggregation_CrossTypeAmountFieldsAreZero() {
    log.info(
        "========== TEST 10: Promotion Aggregation - Cross-type amounts default to '0' when NULL ==========");

    createTestTransaction("PROMO", "PROMO-003", LocalDate.now().minusDays(1));
    Long transactionId = getLastInsertedId("transaction");
    createTestTransactionLine(transactionId, "PROMO-LINE-003", "DISCOUNT", "0004");
    Long lineId = getLastInsertedId("transaction_line");
    createTestPromotionTransactionLine(lineId, "-25.00", "LOYALTY_PROMO");

    var config = AggregationTestDataBuilder.buildPromotionAggregationConfig(false);
    configurationRepository.save(config);

    aggregationService.run();

    var fileDetails = fileDetailRepository.findAll();
    assertThat(fileDetails).isNotEmpty();
    String csvContent = fileDetails.get(0).getFileContent();

    String[] lines = csvContent.split("\n");
    assertThat(lines.length).isGreaterThanOrEqualTo(2);

    String headerLine = lines[0];
    String dataLine = lines[1];

    assertThat(headerLine).contains("LINE_ITEM_AMOUNT");
    assertThat(headerLine).contains("EMPLOYEE_DISCOUNT_AMOUNT");
    assertThat(headerLine).contains("TAX_AMOUNT");
    assertThat(headerLine).contains("TENDER_AMOUNT");
    assertThat(headerLine).contains("MARKETPLACE_JWN_COMMISSION_AMOUNT");
    assertThat(headerLine).contains("WAIVED_AMOUNT");
    assertThat(headerLine).contains("RESTAURANT_TIP_AMOUNT");

    // Verify promotion-specific amount field has actual value
    assertThat(dataLine).contains("-25.00"); // PROMO_AMOUNT

    // Verify all 7 cross-type amount fields are '0' (not empty/NULL)
    String[] values = dataLine.split("\\|", -1);
    String[] headers = headerLine.split("\\|", -1);

    int lineItemIdx = findHeaderIndex(headers, "LINE_ITEM_AMOUNT");
    int employeeDiscountIdx = findHeaderIndex(headers, "EMPLOYEE_DISCOUNT_AMOUNT");
    int taxIdx = findHeaderIndex(headers, "TAX_AMOUNT");
    int tenderIdx = findHeaderIndex(headers, "TENDER_AMOUNT");
    int marketplaceIdx = findHeaderIndex(headers, "MARKETPLACE_JWN_COMMISSION_AMOUNT");
    int waivedIdx = findHeaderIndex(headers, "WAIVED_AMOUNT");
    int restaurantTipIdx = findHeaderIndex(headers, "RESTAURANT_TIP_AMOUNT");

    assertThat(values[lineItemIdx].trim()).isEqualTo("0");
    assertThat(values[employeeDiscountIdx].trim()).isEqualTo("0");
    assertThat(values[taxIdx].trim()).isEqualTo("0");
    assertThat(values[tenderIdx].trim()).isEqualTo("0");
    assertThat(values[marketplaceIdx].trim()).isEqualTo("0");
    assertThat(values[waivedIdx].trim()).isEqualTo("0");
    assertThat(values[restaurantTipIdx].trim()).isEqualTo("0");

    log.info(
        "Test 10 PASSED: Promotion aggregation sets all 7 non-promotion amount fields to '0' (not NULL)");
  }

  // ==================== HELPER METHODS FOR TEST DATA CREATION ====================

  /** Creates a test transaction record */
  private void createTestTransaction(
      String sourceRefSystemType, String sourceRefTxnId, LocalDate processedDate) {
    jdbcTemplate.update(
        """
                INSERT INTO transaction (
                    source_reference_transaction_id, source_reference_system_type, source_reference_type,
                    source_processed_date, transaction_date, business_date, transaction_type,
                    transaction_reversal_code, partner_relationship_type
                ) VALUES (?, ?, 'RETAIL', ?, ?, ?, 'SALE', 'N', NULL)
                """,
        sourceRefTxnId,
        sourceRefSystemType,
        processedDate,
        processedDate,
        processedDate);
  }

  /** Creates a test transaction line record */
  private void createTestTransactionLine(
      Long transactionId, String sourceRefLineId, String lineType, String storeOfIntent) {
    jdbcTemplate.update(
        """
                INSERT INTO transaction_line (
                    transaction_id, source_reference_line_id, source_reference_line_type,
                    transaction_line_type, ringing_store, store_of_intent
                ) VALUES (?, ?, 'TEST', ?, NULL, ?)
                """,
        transactionId,
        sourceRefLineId,
        lineType,
        storeOfIntent);
  }

  /** Creates a test retail transaction line record */
  private void createTestRetailTransactionLine(
      Long transactionLineId,
      String lineItemAmount,
      String employeeDiscountAmount,
      String taxAmount,
      String tenderAmount,
      String deptId,
      String classId) {
    jdbcTemplate.update(
        """
                INSERT INTO retail_transaction_line (
                    transaction_line_id, department_id, class_id, fee_code, tender_type,
                    tender_card_type_code, tender_card_subtype_code, tender_capture_type,
                    tender_activity_code, line_item_amount, tax_amount, employee_discount_amount,
                    tender_amount, mid_merchant_id, fee_code_gl_store_flag, fee_code_gl_store_number,
                    fulfillment_type_dropship_code, cash_disbursement_line1, cash_disbursement_line2,
                    waived_reason_code, waived_amount, subclass_grouping
                ) VALUES (?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
                """,
        transactionLineId,
        deptId,
        classId,
        new BigDecimal(lineItemAmount),
        new BigDecimal(taxAmount),
        new BigDecimal(employeeDiscountAmount),
        new BigDecimal(tenderAmount));
  }

  /** Creates a test restaurant transaction line record */
  private void createTestRestaurantTransactionLine(
      Long transactionLineId,
      String lineItemAmount,
      String employeeDiscountAmount,
      String taxAmount,
      String tenderAmount,
      String tipAmount,
      String deptId) {
    jdbcTemplate.update(
        """
                INSERT INTO restaurant_transaction_line (
                    transaction_line_id, line_item_amount, employee_discount_amount, tax_amount,
                    tender_amount, restaurant_tip_amount, department_id, class_id, tender_type,
                    tender_card_type_code, tender_card_subtype_code, tender_capture_type,
                    restaurant_loyalty_benefit_type, restaurant_delivery_partner
                ) VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
                """,
        transactionLineId,
        new BigDecimal(lineItemAmount),
        new BigDecimal(employeeDiscountAmount),
        new BigDecimal(taxAmount),
        new BigDecimal(tenderAmount),
        new BigDecimal(tipAmount),
        deptId);
  }

  /** Creates a test marketplace transaction line record */
  private void createTestMarketplaceTransactionLine(
      Long transactionLineId,
      String lineItemAmount,
      String taxAmount,
      String tenderAmount,
      String commissionAmount) {
    jdbcTemplate.update(
        """
                INSERT INTO marketplace_transaction_line (
                    transaction_line_id, partner_relationship_type, line_item_amount, tax_amount,
                    tender_amount, marketplace_jwn_commission_amount, fee_code, tender_type,
                    refund_adjustment_reason_code
                ) VALUES (?, NULL, ?, ?, ?, ?, NULL, NULL, NULL)
                """,
        transactionLineId,
        new BigDecimal(lineItemAmount),
        new BigDecimal(taxAmount),
        new BigDecimal(tenderAmount),
        new BigDecimal(commissionAmount));
  }

  /** Creates a test promotion transaction line record */
  private void createTestPromotionTransactionLine(
      Long transactionLineId, String promoAmount, String businessOrigin) {
    jdbcTemplate.update(
        """
                INSERT INTO promotion_transaction_line (
                    transaction_line_id, promo_type, promo_amount, promo_business_origin
                ) VALUES (?, NULL, ?, ?)
                """,
        transactionLineId,
        new BigDecimal(promoAmount),
        businessOrigin);
  }

  /**
   * Gets the last inserted ID from a table by querying the appropriate sequence based on the table
   * name.
   */
  private Long getLastInsertedId(String tableName) {
    String sequenceName =
        switch (tableName) {
          case "transaction" -> "transaction_transaction_id_seq";
          case "transaction_line" -> "transaction_line_transaction_line_id_seq";
          case "retail_transaction_line" -> "retail_transaction_line_retail_transaction_line_id_seq";
          case "restaurant_transaction_line" -> "restaurant_transaction_line_restaurant_transaction_line_id_seq";
          case "marketplace_transaction_line" -> "marketplace_transaction_line_marketplace_transaction_line_i_seq";
          case "promotion_transaction_line" -> "promotion_transaction_line_promotion_transaction_line_id_seq";
          default -> throw new IllegalArgumentException("Unknown table: " + tableName);
        };

    return jdbcTemplate.queryForObject("SELECT currval('" + sequenceName + "')", Long.class);
  }

  /**
   * Helper method to find the index of a column in the CSV header.
   *
   * @throws AssertionError if column is not found
   */
  private int findHeaderIndex(String[] headers, String columnName) {
    for (int i = 0; i < headers.length; i++) {
      if (headers[i].trim().equalsIgnoreCase(columnName)) {
        return i;
      }
    }
    throw new AssertionError("Column not found in CSV header: " + columnName);
  }
}
