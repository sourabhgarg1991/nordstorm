package com.nordstrom.finance.dataintegration.transactionaggregator.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper class for cleaning up database tables before integration tests. Deletes all rows from
 * tables in the correct order to respect foreign key constraints.
 */
@Slf4j
@Component
public class DatabaseCleanupHelper {

  @PersistenceContext private EntityManager entityManager;

  /**
   * Deletes all rows from all tables in the correct order to respect FK constraints. Order: child
   * tables first, then parent tables (transaction_aggregation_relation → generated_file_detail →
   * child transaction lines → transaction_line → transaction → aggregation_configuration)
   *
   * <p>Note: aggregation_configuration is deactivated (not deleted) by setting end_date to
   * yesterday, ensuring old configurations don't interfere with new tests while preserving the
   * data.
   */
  @Transactional
  public void cleanAllTables() {
    log.info("Starting database cleanup for integration tests...");

    // Delete in order respecting FK constraints (child -> parent)
    deleteFromTable("transaction_aggregation_relation");
    deleteFromTable("generated_file_detail");
    deleteFromTable("promotion_transaction_line");
    deleteFromTable("retail_transaction_line");
    deleteFromTable("restaurant_transaction_line");
    deleteFromTable("marketplace_transaction_line");
    deleteFromTable("transaction_line");
    deleteFromTable("transaction");

    // Delete only integration test configurations (those with '_Integration_Test' suffix)
    // Load test configurations (without suffix) are never touched and remain active
    deleteIntegrationTestConfigurations();

    log.info("Database cleanup completed successfully");
  }

  /**
   * Deletes only integration test configs with '_Integration_Test' suffix. Load test configs remain
   * active and untouched.
   */
  private void deleteIntegrationTestConfigurations() {
    try {
      String sql =
          "DELETE FROM aggregation_configuration "
              + "WHERE file_name_prefix LIKE '%_Integration_Test'";
      int deleted = entityManager.createNativeQuery(sql).executeUpdate();
      log.debug("Deleted {} integration test configuration(s)", deleted);
    } catch (Exception e) {
      log.warn("Failed to delete integration test configurations: {}", e.getMessage());
    }
  }

  /**
   * Deletes all rows from a specific table.
   *
   * @param tableName the name of the table to delete from
   */
  private void deleteFromTable(String tableName) {
    try {
      String sql = "DELETE FROM " + tableName;
      entityManager.createNativeQuery(sql).executeUpdate();
      log.debug("Deleted all rows from table: {}", tableName);
    } catch (Exception e) {
      log.warn("Failed to delete from table {}: {}", tableName, e.getMessage());
    }
  }
}
