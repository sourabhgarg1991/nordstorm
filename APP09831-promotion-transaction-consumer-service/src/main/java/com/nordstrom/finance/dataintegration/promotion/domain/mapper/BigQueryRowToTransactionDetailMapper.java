package com.nordstrom.finance.dataintegration.promotion.domain.mapper;

import static com.nordstrom.finance.dataintegration.promotion.database.gcp.constant.GcpQueryConstants.*;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.nordstrom.finance.dataintegration.common.util.DateTimeFormatUtility;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.PromotionGroupType;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionActivityCode;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionCode;
import com.nordstrom.finance.dataintegration.promotion.domain.model.LineItemDetailVO;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class BigQueryRowToTransactionDetailMapper {

  /**
   * Maps a BigQuery row to TransactionDetailVO.
   *
   * @param row the BigQuery FieldValueList representing a row with details, business_date, and
   *     global_tran_id
   * @return a TransactionDetailVO mapped from the row
   */
  public static TransactionDetailVO map(FieldValueList row, Schema schema) {
    Field detailsField = schema.getFields().get(DETAILS_FIELD_NAME);
    FieldList detailStructFields = detailsField.getSubFields();

    return new TransactionDetailVO(
        row.get(DETAILS_FIELD_NAME).getRepeatedValue().stream()
            .map(
                v -> {
                  // Reconstruct nested FieldValueList with its schema
                  FieldValueList detailWithSchema =
                      FieldValueList.of(v.getRecordValue(), detailStructFields);
                  return mapLineItem(detailWithSchema);
                })
            .toList(),
        DateTimeFormatUtility.parseSimpleDate(row.get(BUSINESS_DATE_FIELD_NAME).getStringValue()),
        row.get(GLOBAL_TRANS_ID_FIELD_NAME).getStringValue());
  }

  /**
   * Maps a STRUCT field to LineItemDetailVO.
   *
   * @param details the FieldValueList representing a STRUCT of line item details
   * @return a LineItemDetailVO mapped from the struct
   * @throws IllegalArgumentException if enum mapping fails or required fields are missing
   */
  public static LineItemDetailVO mapLineItem(FieldValueList details) {

    return new LineItemDetailVO(
        DateTimeFormatUtility.parseTimestampToDate(
            details.get(FIRST_REPORTED_TMSTP_FIELD).getStringValue()),
        PromotionGroupType.valueOf(details.get(BUSINESS_ORIGIN_FIELD).getStringValue()),
        details.get(ITEM_TRANSACTION_LINE_ID_FIELD).getStringValue(),
        new BigDecimal(details.get(DISCOUNT_FIELD).getNumericValue().toString()).abs(),
        details.get(REVERSAL_FLAG_FIELD).getStringValue().equals("Y"),
        TransactionCode.fromCode(details.get(TRAN_TYPE_CODE_FIELD).getStringValue())
            .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Code")),
        TransactionActivityCode.fromActivityCode(
                details.get(LINE_ITEM_ACTIVITY_TYPE_CODE_FIELD).getStringValue())
            .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Activity Code")),
        details.get(STORE_NUM_FIELD).getStringValue());
  }
}
