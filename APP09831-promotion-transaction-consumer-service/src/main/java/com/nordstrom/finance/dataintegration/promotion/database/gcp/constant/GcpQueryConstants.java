package com.nordstrom.finance.dataintegration.promotion.database.gcp.constant;

import lombok.experimental.UtilityClass;

/** Constants for GCP BigQuery promotion queries and parameter names. */
@UtilityClass
public final class GcpQueryConstants {

  public static final String PROMOTION_FETCH_QUERY_WITH_PLACEHOLDERS =
      "SELECT ARRAY_AGG( STRUCT( first_reported_tmstp, business_origin, "
          + "item_transaction_line_id, discount, reversal_flag, tran_type_code, line_item_activity_type_code, store_num )) as details, "
          + " business_date, global_tran_id "
          + " FROM `%s.%s.%s` WHERE business_origin IN UNNEST (@businessOrigin) AND first_reported_tmstp >= @startDate "
          + "AND first_reported_tmstp < @endDate GROUP BY global_tran_id, business_date";

  public static final String PARAM_BUSINESS_ORIGIN = "businessOrigin";
  public static final String PARAM_START_DATE = "startDate";
  public static final String PARAM_END_DATE = "endDate";

  public static final String DETAILS_FIELD_NAME = "details";
  public static final String BUSINESS_DATE_FIELD_NAME = "business_date";
  public static final String GLOBAL_TRANS_ID_FIELD_NAME = "global_tran_id";

  public static final String FIRST_REPORTED_TMSTP_FIELD = "first_reported_tmstp";
  public static final String BUSINESS_ORIGIN_FIELD = "business_origin";
  public static final String ITEM_TRANSACTION_LINE_ID_FIELD = "item_transaction_line_id";
  public static final String DISCOUNT_FIELD = "discount";
  public static final String REVERSAL_FLAG_FIELD = "reversal_flag";
  public static final String TRAN_TYPE_CODE_FIELD = "tran_type_code";
  public static final String LINE_ITEM_ACTIVITY_TYPE_CODE_FIELD = "line_item_activity_type_code";
  public static final String STORE_NUM_FIELD = "store_num";

  public static final String GCP_EXECUTOR_BEAN_NAME = "promotionTaskExecutor";
}
