INSERT INTO public.aggregation_configuration
(aggregation_configuration_id, file_name_prefix, file_delimiter, is_data_quotes_surrounded, query, start_date, end_date, created_datetime, last_updated_datetime)
VALUES(nextval('aggregation_configuration_aggregation_configuration_id_seq'::regclass), 'Retail_Transaction_Aggregation', '|', false,
'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
    gen_random_uuid() as aggregation_id,
    TRANSACTION_DATE,
    SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
    BUSINESS_DATE,
    TRANSACTION_TYPE,
    TRANSACTION_LINE_TYPE,
    RINGING_STORE,
    STORE_OF_INTENT,
    sum(LINE_ITEM_AMOUNT) as "LINE_ITEM_AMOUNT",
    sum(EMPLOYEE_DISCOUNT_AMOUNT) as "EMPLOYEE_DISCOUNT_AMOUNT",
    sum(TAX_AMOUNT) as "TAX_AMOUNT",
    sum(TENDER_AMOUNT) as "TENDER_AMOUNT",
    DEPARTMENT_ID,
    CLASS_ID,
    FEE_CODE,
    TENDER_TYPE,
    TENDER_CARD_TYPE_CODE,
    TENDER_CARD_SUBTYPE_CODE,
    TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
    null as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
    TENDER_CAPTURE_TYPE ,
    MID_MERCHANT_ID,
    TENDER_ADJUSTMENT_CODE,
    FEE_CODE_GL_STORE_FLAG,
    FEE_CODE_GL_STORE_NUMBER,
    CASH_DISBURSEMENT_LINE1,
    CASH_DISBURSEMENT_LINE2,
    WAIVED_REASON_CODE,
    sum(WAIVED_AMOUNT) as "WAIVED_AMOUNT",
    FULFILLMENT_TYPE_DROPSHIP_CODE,
    null as "REFUND_ADJUSTMENT_REASON_CODE",
    null as "RESTAURANT_TIP_AMOUNT",
    null as "RESTAURANT_LOYALTY_BENEFIT_TYPE",
    null as "RESTAURANT_DELIVERY_PARTNER",
    SUBCLASS_GROUPING,
    null as "PROMO_TYPE",
    null as "PROMO_AMOUNT",
    null as "PROMO_BUSINESS_ORIGIN",
    null as "PARTNER_RELATIONSHIP_TYPE"
 from
    transaction ta
 join Transaction_Line tld
      on
    ta.transaction_id = tld.transaction_id
 join retail_transaction_line rtl
      on
    rtl.transaction_line_id = tld.transaction_line_id
 left join Transaction_Aggregation_Relation tar
      on
    tar.transaction_line_id = tld.transaction_line_id
 where
    1 = 1
    and tar.transaction_line_id is null
    and ta.source_processed_date < date(localtimestamp)
 group by
    TRANSACTION_DATE,
    SOURCE_PROCESSED_DATE ,
    BUSINESS_DATE,
    TRANSACTION_TYPE,
    TRANSACTION_LINE_TYPE,
    RINGING_STORE,
    STORE_OF_INTENT,
    DEPARTMENT_ID,
    CLASS_ID,
    FEE_CODE,
    TENDER_TYPE,
    TENDER_CARD_TYPE_CODE,
    TENDER_CARD_SUBTYPE_CODE,
    TRANSACTION_REVERSAL_CODE,
    TENDER_CAPTURE_TYPE ,
    MID_MERCHANT_ID,
    TENDER_ADJUSTMENT_CODE,
    FEE_CODE_GL_STORE_FLAG,
    FEE_CODE_GL_STORE_NUMBER,
    CASH_DISBURSEMENT_LINE1,
    CASH_DISBURSEMENT_LINE2,
    WAIVED_REASON_CODE,
    FULFILLMENT_TYPE_DROPSHIP_CODE,
    SUBCLASS_GROUPING', CURRENT_DATE, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    INSERT INTO public.aggregation_configuration
    (aggregation_configuration_id, file_name_prefix, file_delimiter, is_data_quotes_surrounded, query, start_date, end_date, created_datetime, last_updated_datetime)
    VALUES(nextval('aggregation_configuration_aggregation_configuration_id_seq'::regclass), 'Restaurant_Transaction_Aggregation', '|', false,
    'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
     	gen_random_uuid() as aggregation_id,
     	TRANSACTION_DATE,
     	SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
     	BUSINESS_DATE,
     	TRANSACTION_TYPE,
     	TRANSACTION_LINE_TYPE,
     	RINGING_STORE,
     	STORE_OF_INTENT,
     	sum(LINE_ITEM_AMOUNT) as "LINE_ITEM_AMOUNT",
     	sum(EMPLOYEE_DISCOUNT_AMOUNT) as "EMPLOYEE_DISCOUNT_AMOUNT",
     	sum(TAX_AMOUNT) as "TAX_AMOUNT",
     	sum(TENDER_AMOUNT) as "TENDER_AMOUNT",
     	DEPARTMENT_ID,
     	CLASS_ID,
     	null as "FEE_CODE",
     	TENDER_TYPE,
     	TENDER_CARD_TYPE_CODE,
     	TENDER_CARD_SUBTYPE_CODE,
     	TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
     	null as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
     	TENDER_CAPTURE_TYPE ,
     	null as "MID_MERCHANT_ID",
     	null as "TENDER_ADJUSTMENT_CODE",
     	null as "FEE_CODE_GL_STORE_FLAG",
     	null as "FEE_CODE_GL_STORE_NUMBER",
     	null as "CASH_DISBURSEMENT_LINE1",
     	null as "CASH_DISBURSEMENT_LINE2",
     	null as "WAIVED_REASON_CODE",
     	null as "WAIVED_AMOUNT",
     	null as "FULFILLMENT_TYPE_DROPSHIP_CODE",
     	null as "REFUND_ADJUSTMENT_REASON_CODE",
     	sum(RESTAURANT_TIP_AMOUNT),
     	RESTAURANT_LOYALTY_BENEFIT_TYPE,
     	RESTAURANT_DELIVERY_PARTNER,
     	null as "SUBCLASS_GROUPING",
     	null as "PROMO_TYPE",
     	null as "PROMO_AMOUNT",
     	null as "PROMO_BUSINESS_ORIGIN",
        null as "PARTNER_RELATIONSHIP_TYPE"
     from
     	transaction ta
     join Transaction_Line tld
          on
     	ta.transaction_id = tld.transaction_id
     join restaurant_transaction_line rtl
          on
     	rtl.transaction_line_id = tld.transaction_line_id
     left join Transaction_Aggregation_Relation tar
          on
     	tar.transaction_line_id = tld.transaction_line_id
     where
     	1 = 1
     	and tar.transaction_line_id is null
     	and ta.source_processed_date < date(localtimestamp)
     group by
     	TRANSACTION_DATE,
     	SOURCE_PROCESSED_DATE ,
     	BUSINESS_DATE,
     	TRANSACTION_TYPE,
     	TRANSACTION_LINE_TYPE,
     	RINGING_STORE,
     	STORE_OF_INTENT,
     	DEPARTMENT_ID,
     	CLASS_ID,
     	TENDER_TYPE,
     	TENDER_CARD_TYPE_CODE,
     	TENDER_CARD_SUBTYPE_CODE,
     	TRANSACTION_REVERSAL_CODE,
     	TENDER_CAPTURE_TYPE,
         RESTAURANT_LOYALTY_BENEFIT_TYPE,
         RESTAURANT_DELIVERY_PARTNER', CURRENT_DATE, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO public.aggregation_configuration
    (aggregation_configuration_id, file_name_prefix, file_delimiter, is_data_quotes_surrounded, query, start_date, end_date, created_datetime, last_updated_datetime)
    VALUES(nextval('aggregation_configuration_aggregation_configuration_id_seq'::regclass), 'Marketplace_Transaction_Aggregation', '|', false,
    'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
     	gen_random_uuid() as aggregation_id,
     	TRANSACTION_DATE,
     	SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
     	BUSINESS_DATE,
     	TRANSACTION_TYPE,
     	TRANSACTION_LINE_TYPE,
     	RINGING_STORE,
     	STORE_OF_INTENT,
     	sum(LINE_ITEM_AMOUNT) as "LINE_ITEM_AMOUNT",
     	null as "EMPLOYEE_DISCOUNT_AMOUNT",
     	sum(TAX_AMOUNT) as "TAX_AMOUNT",
     	sum(TENDER_AMOUNT) as "TENDER_AMOUNT",
        null as "DEPARTMENT_ID",
     	null as "CLASS_ID",
     	FEE_CODE,
     	TENDER_TYPE,
     	null as "TENDER_CARD_TYPE_CODE",
     	null as "TENDER_CARD_SUBTYPE_CODE",
     	TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
     	sum(MARKETPLACE_JWN_COMMISSION_AMOUNT) as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
     	null as "TENDER_CAPTURE_TYPE" ,
     	null as "MID_MERCHANT_ID",
     	null as "TENDER_ADJUSTMENT_CODE",
     	null as "FEE_CODE_GL_STORE_FLAG",
     	null as "FEE_CODE_GL_STORE_NUMBER",
     	null as "CASH_DISBURSEMENT_LINE1",
     	null as "CASH_DISBURSEMENT_LINE2",
     	null as "WAIVED_REASON_CODE",
     	null as "WAIVED_AMOUNT",
     	null as "FULFILLMENT_TYPE_DROPSHIP_CODE",
     	REFUND_ADJUSTMENT_REASON_CODE,
     	null as "RESTAURANT_TIP_AMOUNT",
     	null as "RESTAURANT_LOYALTY_BENEFIT_TYPE",
     	null as "RESTAURANT_DELIVERY_PARTNER",
     	null as "SUBCLASS_GROUPING",
     	null as "PROMO_TYPE",
     	null as "PROMO_AMOUNT",
     	null as "PROMO_BUSINESS_ORIGIN",
         ta.PARTNER_RELATIONSHIP_TYPE
     from
     	transaction ta
     join Transaction_Line tld
          on
     	ta.transaction_id = tld.transaction_id
     join marketplace_transaction_line rtl
          on
     	rtl.transaction_line_id = tld.transaction_line_id
     left join Transaction_Aggregation_Relation tar
          on
     	tar.transaction_line_id = tld.transaction_line_id
     where
     	1 = 1
     	and tar.transaction_line_id is null
     	and ta.source_processed_date < date(localtimestamp)
     group by
     	TRANSACTION_DATE,
     	SOURCE_PROCESSED_DATE ,
     	BUSINESS_DATE,
     	TRANSACTION_TYPE,
     	TRANSACTION_LINE_TYPE,
     	RINGING_STORE,
     	STORE_OF_INTENT,
     	FEE_CODE,
     	TENDER_TYPE,
     	TRANSACTION_REVERSAL_CODE,
         REFUND_ADJUSTMENT_REASON_CODE,
         ta.PARTNER_RELATIONSHIP_TYPE', CURRENT_DATE, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO public.aggregation_configuration
    (aggregation_configuration_id, file_name_prefix, file_delimiter, is_data_quotes_surrounded, query, start_date, end_date, created_datetime, last_updated_datetime)
    VALUES(nextval('aggregation_configuration_aggregation_configuration_id_seq'::regclass), 'Promotion_Transaction_Aggregation', '|', false,
    'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
     	gen_random_uuid() as aggregation_id,
     	TRANSACTION_DATE,
     	SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
     	BUSINESS_DATE,
     	TRANSACTION_TYPE,
     	TRANSACTION_LINE_TYPE,
     	RINGING_STORE,
     	STORE_OF_INTENT,
     	null as  LINE_ITEM_AMOUNT,
     	null as "EMPLOYEE_DISCOUNT_AMOUNT",
     	null as TAX_AMOUNT,
     	null as TENDER_AMOUNT,
        null as DEPARTMENT_ID,
     	null as CLASS_ID,
     	null as FEE_CODE,
     	null as TENDER_TYPE,
     	null as "TENDER_CARD_TYPE_CODE",
     	null as "TENDER_CARD_SUBTYPE_CODE",
     	TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
     	null as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
     	null as "TENDER_CAPTURE_TYPE" ,
     	null as "MID_MERCHANT_ID",
     	null as "TENDER_ADJUSTMENT_CODE",
     	null as "FEE_CODE_GL_STORE_FLAG",
     	null as "FEE_CODE_GL_STORE_NUMBER",
     	null as "CASH_DISBURSEMENT_LINE1",
     	null as "CASH_DISBURSEMENT_LINE2",
     	null as "WAIVED_REASON_CODE",
     	null as "WAIVED_AMOUNT",
     	null as "FULFILLMENT_TYPE_DROPSHIP_CODE",
     	null as "REFUND_ADJUSTMENT_REASON_CODE",
     	null as "RESTAURANT_TIP_AMOUNT",
     	null as "RESTAURANT_LOYALTY_BENEFIT_TYPE",
     	null as "RESTAURANT_DELIVERY_PARTNER",
     	null as "SUBCLASS_GROUPING",
     	PROMO_TYPE,
     	sum(PROMO_AMOUNT) as "PROMO_AMOUNT",
     	PROMO_BUSINESS_ORIGIN,
        null as PARTNER_RELATIONSHIP_TYPE
     from
     	transaction ta
     join Transaction_Line tld
          on
     	ta.transaction_id = tld.transaction_id
     join promotion_transaction_line rtl
          on
     	rtl.transaction_line_id = tld.transaction_line_id
     left join Transaction_Aggregation_Relation tar
          on
     	tar.transaction_line_id = tld.transaction_line_id
     where
     	1 = 1
     	and tar.transaction_line_id is null
     	and ta.source_processed_date < date(localtimestamp)
     group by
     	TRANSACTION_DATE,
     	SOURCE_PROCESSED_DATE ,
     	BUSINESS_DATE,
     	TRANSACTION_TYPE,
     	TRANSACTION_LINE_TYPE,
     	RINGING_STORE,
     	STORE_OF_INTENT,
     	TRANSACTION_REVERSAL_CODE,
        PROMO_TYPE, PROMO_BUSINESS_ORIGIN', CURRENT_DATE, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);