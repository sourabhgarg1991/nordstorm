package com.nordstrom.finance.dataintegration.transactionaggregator.util;

import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import java.time.LocalDate;
import lombok.experimental.UtilityClass;

/**
 * Test data builder for creating aggregation configuration entities in integration tests. Provides
 * realistic SQL queries based on actual production configurations. Each configuration gets an
 * "_Integration_Test" suffix to distinguish from load test configs.
 */
@UtilityClass
public class AggregationTestDataBuilder {

  private static final String INTEGRATION_TEST_SUFFIX = "_Integration_Test";

  /**
   * Creates a Retail Transaction Aggregation configuration for testing. This configuration
   * aggregates retail transaction lines.
   *
   * @return AggregationConfigurationEntity for retail aggregation
   */
  public static AggregationConfigurationEntity buildRetailAggregationConfig(
      boolean hasControlQuery) {
    return AggregationConfigurationEntity.builder()
        .fileNamePrefix("Retail_Transaction_Aggregation" + INTEGRATION_TEST_SUFFIX)
        .fileDelimiter('|')
        .isDataQuotesSurrounded(false)
        .aggregationQuery(getRetailAggregationQuery())
        .dataControlQuery(hasControlQuery ? getRetailControlDataQuery() : null)
        .startDate(LocalDate.now().minusDays(1))
        .endDate(null) // Active indefinitely
        .build();
  }

  /**
   * Creates a Restaurant Transaction Aggregation configuration for testing. This configuration
   * aggregates restaurant transaction lines.
   *
   * @return AggregationConfigurationEntity for restaurant aggregation
   */
  public static AggregationConfigurationEntity buildRestaurantAggregationConfig(
      boolean hasControlQuery) {
    return AggregationConfigurationEntity.builder()
        .fileNamePrefix("Restaurant_Transaction_Aggregation" + INTEGRATION_TEST_SUFFIX)
        .fileDelimiter('|')
        .isDataQuotesSurrounded(false)
        .aggregationQuery(getRestaurantAggregationQuery())
        .dataControlQuery(hasControlQuery ? getRestaurantControlDataQuery() : null)
        .startDate(LocalDate.now().minusDays(1))
        .endDate(null)
        .build();
  }

  /**
   * Creates a Marketplace Transaction Aggregation configuration for testing. This configuration
   * aggregates marketplace transaction lines.
   *
   * @return AggregationConfigurationEntity for marketplace aggregation
   */
  public static AggregationConfigurationEntity buildMarketplaceAggregationConfig(
      boolean hasControlQuery) {
    return AggregationConfigurationEntity.builder()
        .fileNamePrefix("Marketplace_Transaction_Aggregation" + INTEGRATION_TEST_SUFFIX)
        .fileDelimiter('|')
        .isDataQuotesSurrounded(false)
        .aggregationQuery(getMarketplaceAggregationQuery())
        .dataControlQuery(hasControlQuery ? getMarketplaceControlQuery() : null)
        .startDate(LocalDate.now().minusDays(1))
        .endDate(null)
        .build();
  }

  /**
   * Creates a Promotion Transaction Aggregation configuration for testing. This configuration
   * aggregates promotion transaction lines.
   *
   * @return AggregationConfigurationEntity for promotion aggregation
   */
  public static AggregationConfigurationEntity buildPromotionAggregationConfig(
      boolean hasControlQuery) {
    return AggregationConfigurationEntity.builder()
        .fileNamePrefix("Promotion_Transaction_Aggregation" + INTEGRATION_TEST_SUFFIX)
        .fileDelimiter('|')
        .isDataQuotesSurrounded(false)
        .aggregationQuery(getPromotionAggregationQuery())
        .dataControlQuery(hasControlQuery ? getPromotionControlQuery() : null)
        .startDate(LocalDate.now().minusDays(1))
        .endDate(null)
        .build();
  }

  /**
   * Creates a configuration that will return no results (for empty result testing). Uses a
   * aggregationQuery with an impossible WHERE condition.
   *
   * @return AggregationConfigurationEntity that returns no results
   */
  public static AggregationConfigurationEntity buildEmptyResultConfig() {
    return AggregationConfigurationEntity.builder()
        .fileNamePrefix("Empty_Result_Test" + INTEGRATION_TEST_SUFFIX)
        .fileDelimiter(',')
        .isDataQuotesSurrounded(false)
        .aggregationQuery(
            "SELECT STRING_AGG(cast(tld.transaction_line_id as TEXT), ',') as transaction_line_ids, "
                + "gen_random_uuid() as \"AGGREGATION_ID\", "
                + "ta.source_reference_type as \"DATA_SOURCE\" "
                + "FROM transaction ta "
                + "JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id "
                + "WHERE 1=0") // Impossible condition
        .startDate(LocalDate.now().minusDays(1))
        .endDate(null)
        .build();
  }

  // ==================== SQL QUERIES ====================

  /**
   * Returns the SQL aggregationQuery for retail transaction aggregation. Simplified version for
   * testing that focuses on essential fields.
   */
  private static String getRetailAggregationQuery() {
    return """
        SELECT
            STRING_AGG(cast(tld.transaction_line_id as TEXT), ',') as transaction_line_ids,
            gen_random_uuid() as "AGGREGATION_ID",
            ta.source_reference_type as "DATA_SOURCE",
            ta.transaction_date as "TRANSACTION_DATE",
            ta.source_processed_date as "SALES_SYSTEM_PROCESS_DATE",
            ta.business_date as "BUSINESS_DATE",
            ta.transaction_type as "TRANSACTION_TYPE",
            tld.transaction_line_type as "TRANSACTION_LINE_TYPE",
            tld.ringing_store as "RINGING_STORE",
            tld.store_of_intent as "STORE_OF_INTENT",
            SUM(rtl.line_item_amount) as "LINE_ITEM_AMOUNT",
            SUM(rtl.employee_discount_amount) as "EMPLOYEE_DISCOUNT_AMOUNT",
            SUM(rtl.tax_amount) as "TAX_AMOUNT",
            SUM(rtl.tender_amount) as "TENDER_AMOUNT",
            rtl.department_id as "DEPT_ID",
            rtl.class_id as "CLASS_ID",
            '0' as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
            '0' as "RESTAURANT_TIP_AMOUNT",
            '0' as "PROMO_AMOUNT"
        FROM transaction ta
        JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
        JOIN retail_transaction_line rtl ON rtl.transaction_line_id = tld.transaction_line_id
        LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
        WHERE tar.transaction_line_id IS NULL
            AND ta.source_processed_date < CURRENT_DATE
        GROUP BY
            ta.source_reference_type,
            ta.transaction_date,
            ta.source_processed_date,
            ta.business_date,
            ta.transaction_type,
            tld.transaction_line_type,
            tld.ringing_store,
            tld.store_of_intent,
            rtl.department_id,
            rtl.class_id
        """;
  }

  private static String getRetailControlDataQuery() {
    return """
            SELECT
                COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
                COALESCE(sum(EMPLOYEE_DISCOUNT_AMOUNT), 0) +
                COALESCE(sum(TAX_AMOUNT), 0) +
                COALESCE(sum(TENDER_AMOUNT), 0) +
                COALESCE(sum(WAIVED_AMOUNT), 0) as "TOTAL_AMOUNTS"
            FROM transaction ta
            JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
            JOIN retail_transaction_line rtl ON rtl.transaction_line_id = tld.transaction_line_id
            LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
            WHERE tar.transaction_line_id IS NULL
                AND ta.source_processed_date < CURRENT_DATE
            """;
  }

  /**
   * Returns the SQL aggregationQuery for restaurant transaction aggregation. Simplified version for
   * testing.
   */
  private static String getRestaurantAggregationQuery() {
    return """
        SELECT
            STRING_AGG(cast(tld.transaction_line_id as TEXT), ',') as transaction_line_ids,
            gen_random_uuid() as "AGGREGATION_ID",
            ta.source_reference_type as "DATA_SOURCE",
            ta.transaction_date as "TRANSACTION_DATE",
            ta.source_processed_date as "SALES_SYSTEM_PROCESS_DATE",
            ta.business_date as "BUSINESS_DATE",
            ta.transaction_type as "TRANSACTION_TYPE",
            tld.transaction_line_type as "TRANSACTION_LINE_TYPE",
            tld.store_of_intent as "STORE_OF_INTENT",
            SUM(rtl.line_item_amount) as "LINE_ITEM_AMOUNT",
            SUM(rtl.tax_amount) as "TAX_AMOUNT",
            SUM(rtl.tender_amount) as "TENDER_AMOUNT",
            SUM(rtl.restaurant_tip_amount) as "RESTAURANT_TIP_AMOUNT",
            rtl.department_id as "DEPT_ID",
            '0' as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
            '0' as "WAIVED_AMOUNT",
            '0' as "PROMO_AMOUNT"
        FROM transaction ta
        JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
        JOIN restaurant_transaction_line rtl ON rtl.transaction_line_id = tld.transaction_line_id
        LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
        WHERE tar.transaction_line_id IS NULL
            AND ta.source_processed_date < CURRENT_DATE
        GROUP BY
            ta.source_reference_type,
            ta.transaction_date,
            ta.source_processed_date,
            ta.business_date,
            ta.transaction_type,
            tld.transaction_line_type,
            tld.store_of_intent,
            rtl.department_id
        """;
  }

  private static String getRestaurantControlDataQuery() {
    return """
        SELECT
        	COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
            COALESCE(sum(EMPLOYEE_DISCOUNT_AMOUNT), 0)  +
            COALESCE(sum(TAX_AMOUNT), 0) +
            COALESCE(sum(TENDER_AMOUNT), 0)  +
            COALESCE(sum(RESTAURANT_TIP_AMOUNT), 0)  as "TOTAL_AMOUNTS"
        FROM transaction ta
        JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
        JOIN restaurant_transaction_line rtl ON rtl.transaction_line_id = tld.transaction_line_id
        LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
        WHERE tar.transaction_line_id IS NULL
            AND ta.source_processed_date < CURRENT_DATE
        """;
  }

  /**
   * Returns the SQL aggregationQuery for marketplace transaction aggregation. Simplified version
   * for testing.
   */
  private static String getMarketplaceAggregationQuery() {
    return """
        SELECT
            STRING_AGG(cast(tld.transaction_line_id as TEXT), ',') as transaction_line_ids,
            gen_random_uuid() as "AGGREGATION_ID",
            ta.source_reference_type as "DATA_SOURCE",
            ta.transaction_date as "TRANSACTION_DATE",
            ta.source_processed_date as "SALES_SYSTEM_PROCESS_DATE",
            ta.business_date as "BUSINESS_DATE",
            ta.transaction_type as "TRANSACTION_TYPE",
            tld.transaction_line_type as "TRANSACTION_LINE_TYPE",
            tld.store_of_intent as "STORE_OF_INTENT",
            SUM(mtl.line_item_amount) as "LINE_ITEM_AMOUNT",
            SUM(mtl.tax_amount) as "TAX_AMOUNT",
            SUM(mtl.tender_amount) as "TENDER_AMOUNT",
            SUM(mtl.marketplace_jwn_commission_amount) as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
            ta.partner_relationship_type as "PARTNER_RELATIONSHIP_TYPE",
            '0' as "EMPLOYEE_DISCOUNT_AMOUNT",
            '0' as "WAIVED_AMOUNT",
            '0' as "RESTAURANT_TIP_AMOUNT",
            '0' as "PROMO_AMOUNT"
        FROM transaction ta
        JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
        JOIN marketplace_transaction_line mtl ON mtl.transaction_line_id = tld.transaction_line_id
        LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
        WHERE tar.transaction_line_id IS NULL
            AND ta.source_processed_date < CURRENT_DATE
        GROUP BY
            ta.source_reference_type,
            ta.transaction_date,
            ta.source_processed_date,
            ta.business_date,
            ta.transaction_type,
            tld.transaction_line_type,
            tld.store_of_intent,
            ta.partner_relationship_type
        """;
  }

  private static String getMarketplaceControlQuery() {
    return """
            SELECT
                COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
                COALESCE(sum(TAX_AMOUNT), 0)  +
                COALESCE(sum(TENDER_AMOUNT), 0)  +
                COALESCE(sum(MARKETPLACE_JWN_COMMISSION_AMOUNT), 0)  as "TOTAL_AMOUNTS"
            FROM transaction ta
            JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
            JOIN marketplace_transaction_line mtl ON mtl.transaction_line_id = tld.transaction_line_id
            LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
            WHERE tar.transaction_line_id IS NULL
                AND ta.source_processed_date < CURRENT_DATE
            """;
  }

  /**
   * Returns the SQL aggregationQuery for promotion transaction aggregation. Simplified version for
   * testing.
   */
  private static String getPromotionAggregationQuery() {
    return """
        SELECT
            STRING_AGG(cast(tld.transaction_line_id as TEXT), ',') as transaction_line_ids,
            gen_random_uuid() as "AGGREGATION_ID",
            ta.source_reference_type as "DATA_SOURCE",
            ta.transaction_date as "TRANSACTION_DATE",
            ta.source_processed_date as "SALES_SYSTEM_PROCESS_DATE",
            ta.business_date as "BUSINESS_DATE",
            ta.transaction_type as "TRANSACTION_TYPE",
            tld.transaction_line_type as "TRANSACTION_LINE_TYPE",
            tld.store_of_intent as "STORE_OF_INTENT",
            SUM(ptl.promo_amount) as "PROMO_AMOUNT",
            ptl.promo_business_origin as "PROMO_BUSINESS_ORIGIN",
            '0' as "LINE_ITEM_AMOUNT",
            '0' as "EMPLOYEE_DISCOUNT_AMOUNT",
            '0' as "TAX_AMOUNT",
            '0' as "TENDER_AMOUNT",
            '0' as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
            '0' as "WAIVED_AMOUNT",
            '0' as "RESTAURANT_TIP_AMOUNT"
        FROM transaction ta
        JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
        JOIN promotion_transaction_line ptl ON ptl.transaction_line_id = tld.transaction_line_id
        LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
        WHERE tar.transaction_line_id IS NULL
            AND ta.source_processed_date < CURRENT_DATE
        GROUP BY
            ta.source_reference_type,
            ta.transaction_date,
            ta.source_processed_date,
            ta.business_date,
            ta.transaction_type,
            tld.transaction_line_type,
            tld.store_of_intent,
            ptl.promo_business_origin
        """;
  }

  /**
   * Returns the SQL aggregationQuery for promotion transaction aggregation. Simplified version for
   * testing.
   */
  private static String getPromotionControlQuery() {
    return """
        SELECT
            SUM(ptl.promo_amount) as "PROMO_AMOUNT"
        FROM transaction ta
        JOIN transaction_line tld ON ta.transaction_id = tld.transaction_id
        JOIN promotion_transaction_line ptl ON ptl.transaction_line_id = tld.transaction_line_id
        LEFT JOIN transaction_aggregation_relation tar ON tar.transaction_line_id = tld.transaction_line_id
        WHERE tar.transaction_line_id IS NULL
            AND ta.source_processed_date < CURRENT_DATE
        """;
  }
}
