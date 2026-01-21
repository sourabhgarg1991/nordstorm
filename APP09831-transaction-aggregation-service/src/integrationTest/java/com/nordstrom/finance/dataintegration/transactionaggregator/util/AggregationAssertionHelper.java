package com.nordstrom.finance.dataintegration.transactionaggregator.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.TransactionAggregationRelationEntity;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Helper class for assertions in aggregation integration tests. Provides reusable assertion methods
 * for verifying aggregation results.
 */
@UtilityClass
public class AggregationAssertionHelper {

  /**
   * Asserts that a GeneratedFileDetailEntity has all expected fields populated correctly.
   *
   * @param fileDetail the generated file detail to assert
   * @param expectedFileNamePrefix the expected file name prefix
   * @param expectedDelimiter the expected CSV delimiter
   * @param configId the configuration ID that generated this file
   */
  public static void assertGeneratedFileDetail(
      GeneratedFileDetailEntity fileDetail,
      String expectedFileNamePrefix,
      char expectedDelimiter,
      Long configId) {

    assertThat(fileDetail).as("Generated file detail should not be null").isNotNull();

    assertThat(fileDetail.getGeneratedFileDetailId())
        .as("Generated file detail ID should be generated")
        .isNotNull();

    assertThat(fileDetail.getGeneratedFileName())
        .as("Generated file name should not be null")
        .isNotNull()
        .startsWith(expectedFileNamePrefix)
        .endsWith(".csv");

    assertThat(fileDetail.getFileContent())
        .as("File content should not be null or empty")
        .isNotNull()
        .isNotEmpty();

    assertThat(fileDetail.getAggregationConfigurationId())
        .as("Aggregation configuration ID should match")
        .isEqualTo(configId);

    assertThat(fileDetail.getCreatedDatetime()).as("Created datetime should be set").isNotNull();

    assertThat(fileDetail.getLastUpdatedDatetime())
        .as("Last updated datetime should be set")
        .isNotNull();

    // Verify CSV delimiter is used in content
    assertThat(fileDetail.getFileContent())
        .as("File content should contain the delimiter")
        .contains(String.valueOf(expectedDelimiter));
  }

  /**
   * Asserts that a CSV file content has the expected structure and format.
   *
   * @param fileContent the CSV file content to verify
   * @param delimiter the expected delimiter
   * @param shouldHaveQuotes whether data should be quoted
   * @param minRows minimum expected number of rows (including header)
   */
  public static void assertCSVFileContent(
      String fileContent, char delimiter, boolean shouldHaveQuotes, int minRows) {

    assertThat(fileContent).as("File content should not be null").isNotNull();

    String[] lines = fileContent.split("\n");
    assertThat(lines.length)
        .as("File should have at least " + minRows + " rows")
        .isGreaterThanOrEqualTo(minRows);

    // Verify delimiter usage
    String firstLine = lines[0];
    assertThat(firstLine)
        .as("Header row should contain delimiter")
        .contains(String.valueOf(delimiter));

    // Verify quote usage if expected
    if (shouldHaveQuotes) {
      assertThat(firstLine).as("Content should be quoted").contains("\"");
    }

    // Verify header doesn't contain transaction_line_ids or aggregation_id (they should be
    // stripped)
    assertThat(firstLine.toLowerCase())
        .as("Header should not contain transaction_line_ids (internal field)")
        .doesNotContain("transaction_line_ids");
  }

  /**
   * Asserts that TransactionAggregationRelationEntity records are correctly created.
   *
   * @param relations the list of aggregation relations to verify
   * @param expectedMinCount minimum expected number of relations
   */
  public static void assertAggregationRelations(
      List<TransactionAggregationRelationEntity> relations, int expectedMinCount) {

    assertThat(relations).as("Aggregation relations should not be null").isNotNull();

    assertThat(relations.size())
        .as("Should have at least " + expectedMinCount + " relations")
        .isGreaterThanOrEqualTo(expectedMinCount);

    for (TransactionAggregationRelationEntity relation : relations) {
      assertThat(relation.getTransactionAggregationRelationId())
          .as("Relation ID should be generated")
          .isNotNull();

      assertThat(relation.getAggregationId())
          .as("Aggregation ID should be a valid UUID")
          .isNotNull();

      assertThat(relation.getTransactionLineId())
          .as("Transaction line ID should be set")
          .isNotNull()
          .isPositive();

      assertThat(relation.getIsPublishedToDataPlatform())
          .as("New relations should not be published initially")
          .isFalse();

      assertThat(relation.getCreatedDatetime()).as("Created datetime should be set").isNotNull();

      assertThat(relation.getLastUpdatedDatetime())
          .as("Last updated datetime should be set")
          .isNotNull();
    }
  }

  /**
   * Asserts that aggregation was successful by verifying both file details and relations exist.
   *
   * @param fileDetails list of generated file details
   * @param relations list of aggregation relations
   * @param expectedFileCount expected number of files
   * @param expectedMinRelations minimum expected number of relations
   */
  public static void assertAggregationSuccess(
      List<GeneratedFileDetailEntity> fileDetails,
      List<TransactionAggregationRelationEntity> relations,
      int expectedFileCount,
      int expectedMinRelations) {

    assertThat(fileDetails).as("Should have generated file details").hasSize(expectedFileCount);

    assertThat(relations).as("Should have aggregation relations").isNotNull();

    assertThat(relations.size())
        .as("Should have at least " + expectedMinRelations + " relations")
        .isGreaterThanOrEqualTo(expectedMinRelations);

    // All file details should have content
    assertThat(fileDetails)
        .as("All files should have content")
        .allMatch(fd -> fd.getFileContent() != null && !fd.getFileContent().isEmpty());

    assertThat(fileDetails)
        .as("All files must be uploaded")
        .allMatch(GeneratedFileDetailEntity::getIsUploadedToS3);

    // All relations should be unpublished initially
    assertThat(relations)
        .as("All new relations should be unpublished")
        .allMatch(r -> !r.getIsPublishedToDataPlatform());
  }

  /**
   * Asserts that no files or relations were created (for empty result scenarios).
   *
   * @param fileDetails list of generated file details
   * @param relations list of aggregation relations
   */
  public static void assertNoAggregationResults(
      List<GeneratedFileDetailEntity> fileDetails,
      List<TransactionAggregationRelationEntity> relations) {

    assertThat(fileDetails).as("Should not have generated any files for empty result").isEmpty();

    assertThat(relations).as("Should not have created any relations for empty result").isEmpty();
  }
}
