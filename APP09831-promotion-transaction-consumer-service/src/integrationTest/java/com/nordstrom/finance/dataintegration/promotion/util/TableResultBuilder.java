package com.nordstrom.finance.dataintegration.promotion.util;

import com.google.api.gax.paging.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.mockito.Mockito;

/**
 * Builder utility for creating mock TableResult objects for BigQuery testing. Pre-creates and
 * configures mocks to avoid UnfinishedStubbingException.
 */
@UtilityClass
public class TableResultBuilder {

  /**
   * Builds a TableResult with the provided rows (varargs).
   *
   * @param rows the rows to include in the result
   * @return a mocked TableResult containing the specified rows
   */
  public static TableResult buildWithRows(FieldValueList... rows) {
    return buildWithRows(Arrays.asList(rows));
  }

  /**
   * Builds a TableResult with the provided list of rows. Uses doReturn().when() syntax to avoid
   * UnfinishedStubbingException.
   *
   * @param rows the list of rows to include in the result
   * @return a mocked TableResult containing the specified rows
   */
  public static TableResult buildWithRows(List<FieldValueList> rows) {
    // Create the mock FIRST
    TableResult tableResult = Mockito.mock(TableResult.class);

    // Create the page with data
    Page<FieldValueList> page = createPage(rows);

    // Get schema from SourceRowTestDataBuilder
    Schema schema = SourceRowTestDataBuilder.getSourceSchema();

    // Use doReturn().when() instead of when().thenReturn() to avoid evaluation issues
    Mockito.doReturn(page.getValues()).when(tableResult).getValues();
    Mockito.doReturn(null).when(tableResult).getNextPageToken();
    Mockito.doReturn((long) rows.size()).when(tableResult).getTotalRows();
    Mockito.doReturn(false).when(tableResult).hasNextPage();
    Mockito.doReturn(schema).when(tableResult).getSchema();

    return tableResult;
  }

  /**
   * Builds an empty TableResult with no rows.
   *
   * @return a mocked TableResult with no data
   */
  public static TableResult buildEmpty() {
    return buildWithRows(Collections.emptyList());
  }

  /**
   * Creates a Page implementation for BigQuery results.
   *
   * @param values the values in the page
   * @return a Page containing the specified values
   */
  private static Page<FieldValueList> createPage(List<FieldValueList> values) {
    return new PageImpl<>(null, null, values);
  }
}
