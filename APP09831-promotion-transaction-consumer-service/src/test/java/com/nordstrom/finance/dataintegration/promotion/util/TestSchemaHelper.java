package com.nordstrom.finance.dataintegration.promotion.util;

import static com.nordstrom.finance.dataintegration.promotion.database.gcp.constant.GcpQueryConstants.*;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import lombok.experimental.UtilityClass;

/**
 * Helper class to create BigQuery schemas for unit testing. This avoids circular dependencies with
 * integration test utilities.
 */
@UtilityClass
public class TestSchemaHelper {

  /**
   * Creates the schema for the top-level query result row. Matches the structure: SELECT
   * ARRAY_AGG(STRUCT(...)) as details, business_date, global_tran_id
   */
  public static Schema getRowSchema() {
    return Schema.of(
        Field.newBuilder(DETAILS_FIELD_NAME, StandardSQLTypeName.STRUCT, getDetailStructFields())
            .setMode(Field.Mode.REPEATED)
            .build(),
        Field.of(BUSINESS_DATE_FIELD_NAME, StandardSQLTypeName.STRING),
        Field.of(GLOBAL_TRANS_ID_FIELD_NAME, StandardSQLTypeName.STRING));
  }

  /** Creates the schema for the nested STRUCT fields inside the details array. */
  public static FieldList getDetailStructFields() {
    return FieldList.of(
        Field.of(FIRST_REPORTED_TMSTP_FIELD, StandardSQLTypeName.TIMESTAMP),
        Field.of(BUSINESS_ORIGIN_FIELD, StandardSQLTypeName.STRING),
        Field.of(ITEM_TRANSACTION_LINE_ID_FIELD, StandardSQLTypeName.STRING),
        Field.of(DISCOUNT_FIELD, StandardSQLTypeName.NUMERIC),
        Field.of(REVERSAL_FLAG_FIELD, StandardSQLTypeName.STRING),
        Field.of(TRAN_TYPE_CODE_FIELD, StandardSQLTypeName.STRING),
        Field.of(LINE_ITEM_ACTIVITY_TYPE_CODE_FIELD, StandardSQLTypeName.STRING),
        Field.of(STORE_NUM_FIELD, StandardSQLTypeName.STRING));
  }
}
