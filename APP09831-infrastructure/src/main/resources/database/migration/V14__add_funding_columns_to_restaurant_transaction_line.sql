-- 1. Add three new columns to restaurant_transaction_line table
ALTER TABLE "restaurant_transaction_line" ADD COLUMN IF NOT EXISTS funding_type VARCHAR(100);
ALTER TABLE "restaurant_transaction_line" ADD COLUMN IF NOT EXISTS funding_amount DECIMAL;
ALTER TABLE "restaurant_transaction_line" ADD COLUMN IF NOT EXISTS restaurant_concept_name VARCHAR(100);

-- 2. Archive existing aggregation_configuration entries by setting end_date
UPDATE public.aggregation_configuration
SET end_date = CURRENT_DATE,
    last_updated_datetime = CURRENT_TIMESTAMP
WHERE file_name_prefix IN ('JWN_SALES_RETAIL', 'JWN_SALES_RESTAURANT', 'JWN_SALES_MARKETPLACE', 'JWN_SALES_PROMO')
  AND end_date IS NULL;

-- 3. Insert new aggregation_configuration entries
-- 3.1. JWN_SALES_RESTAURANT (actual values)
INSERT INTO public.aggregation_configuration
(file_name_prefix, file_delimiter, is_data_quotes_surrounded, aggregation_query, start_date, end_date, data_control_query)
VALUES (
'JWN_SALES_RESTAURANT',
'|',
false,
'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
             	gen_random_uuid() as "AGGREGATION_ID",
             	SOURCE_REFERENCE_TYPE as "DATA_SOURCE",
             	TRANSACTION_DATE as "TRANSACTION_DATE",
             	SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
             	BUSINESS_DATE as "BUSINESS_DATE",
             	TRANSACTION_TYPE as "TRANSACTION_TYPE",
             	TRANSACTION_LINE_TYPE as "TRANSACTION_LINE_TYPE",
             	RINGING_STORE as "RINGING_STORE",
             	STORE_OF_INTENT as "STORE_OF_INTENT",
             	COALESCE(sum(LINE_ITEM_AMOUNT), 0) as "LINE_ITEM_AMOUNT",
             	COALESCE(sum(EMPLOYEE_DISCOUNT_AMOUNT), 0) as "EMPLOYEE_DISCOUNT_AMOUNT",
             	COALESCE(sum(TAX_AMOUNT), 0) as "TAX_AMOUNT",
             	COALESCE(sum(TENDER_AMOUNT), 0) as "TENDER_AMOUNT",
             	DEPARTMENT_ID as "DEPT_ID",
             	CLASS_ID as "CLASS_ID",
             	null as "FEE_CODE",
             	TENDER_TYPE as "TENDER_TYPE",
             	TENDER_CARD_TYPE_CODE as "TENDER_CARD_TYPE_CODE",
             	TENDER_CARD_SUBTYPE_CODE as "TENDER_CARD_SUBTYPE_CODE",
             	TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
             	''0'' as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
             	TENDER_CAPTURE_TYPE as "TENDER_CAPTURE_TYPE",
             	null as "MID_MERCHANT_ID",
             	null as "TENDER_ACTIVITY_CODE",
             	null as "FEE_CODE_GL_STORE_FLAG",
             	null as "FEE_CODE_GL_STORE_NUMBER",
             	null as "CASH_DISBURSEMENT_LINE1",
             	null as "CASH_DISBURSEMENT_LINE2",
             	null as "WAIVED_REASON_CODE",
             	''0'' as "WAIVED_AMOUNT",
             	null as "FULFILLMENT_TYPE_DROPSHIP_CODE",
             	null as "REFUND_ADJUSTMENT_REASON_CODE",
             	COALESCE(sum(RESTAURANT_TIP_AMOUNT), 0) as "RESTAURANT_TIP_AMOUNT",
             	RESTAURANT_LOYALTY_BENEFIT_TYPE as "RESTAURANT_LOYALTY_BENEFIT_TYPE",
             	RESTAURANT_DELIVERY_PARTNER as "RESTAURANT_DELIVERY_PARTNER",
             	''0'' as "PROMO_AMOUNT",
             	null as "PROMO_BUSINESS_ORIGIN",
                null as "PARTNER_RELATIONSHIP_TYPE",
                FUNDING_TYPE as "FUNDING_TYPE",
                COALESCE(sum(FUNDING_AMOUNT), 0) as "FUNDING_AMOUNT",
                RESTAURANT_CONCEPT_NAME as "RESTAURANT_CONCEPT_NAME"
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
             	and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26''
             group by
                SOURCE_REFERENCE_TYPE,
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
                RESTAURANT_DELIVERY_PARTNER,
                FUNDING_TYPE,
                RESTAURANT_CONCEPT_NAME',
CURRENT_DATE + 1,
NULL,
'select
                              	COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
                              	COALESCE(sum(EMPLOYEE_DISCOUNT_AMOUNT), 0)  +
                              	COALESCE(sum(TAX_AMOUNT), 0) +
                              	COALESCE(sum(TENDER_AMOUNT), 0)  +
                              	COALESCE(sum(RESTAURANT_TIP_AMOUNT), 0)  +
                              	COALESCE(sum(FUNDING_AMOUNT), 0)  as "TOTAL_AMOUNTS"
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
                              	and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26'''
);

-- 3.2. JWN_SALES_RETAIL (placeholder for new restaurant fields)
INSERT INTO public.aggregation_configuration
(file_name_prefix, file_delimiter, is_data_quotes_surrounded, aggregation_query, start_date, end_date, data_control_query)
VALUES (
'JWN_SALES_RETAIL',
'|',
false,
'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
            gen_random_uuid() as "AGGREGATION_ID",
            SOURCE_REFERENCE_TYPE as "DATA_SOURCE",
            TRANSACTION_DATE as "TRANSACTION_DATE",
            SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
            BUSINESS_DATE as "BUSINESS_DATE",
            TRANSACTION_TYPE as "TRANSACTION_TYPE",
            TRANSACTION_LINE_TYPE as "TRANSACTION_LINE_TYPE",
            RINGING_STORE as "RINGING_STORE",
            STORE_OF_INTENT as "STORE_OF_INTENT",
            COALESCE(sum(LINE_ITEM_AMOUNT), 0) as "LINE_ITEM_AMOUNT",
            COALESCE(sum(EMPLOYEE_DISCOUNT_AMOUNT), 0) as "EMPLOYEE_DISCOUNT_AMOUNT",
            COALESCE(sum(TAX_AMOUNT), 0) as "TAX_AMOUNT",
            COALESCE(sum(TENDER_AMOUNT), 0) as "TENDER_AMOUNT",
            DEPARTMENT_ID as "DEPT_ID",
            CLASS_ID as "CLASS_ID",
            FEE_CODE as "FEE_CODE",
            TENDER_TYPE as "TENDER_TYPE",
            TENDER_CARD_TYPE_CODE as "TENDER_CARD_TYPE_CODE",
            TENDER_CARD_SUBTYPE_CODE as "TENDER_CARD_SUBTYPE_CODE",
            TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
            ''0'' as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
            TENDER_CAPTURE_TYPE as "TENDER_CAPTURE_TYPE",
            MID_MERCHANT_ID as "MID_MERCHANT_ID",
            TENDER_ACTIVITY_CODE as "TENDER_ACTIVITY_CODE",
            FEE_CODE_GL_STORE_FLAG as "FEE_CODE_GL_STORE_FLAG",
            FEE_CODE_GL_STORE_NUMBER as "FEE_CODE_GL_STORE_NUMBER",
            CASH_DISBURSEMENT_LINE1 as "CASH_DISBURSEMENT_LINE1",
            CASH_DISBURSEMENT_LINE2 as "CASH_DISBURSEMENT_LINE2",
            WAIVED_REASON_CODE as "WAIVED_REASON_CODE",
            COALESCE(sum(WAIVED_AMOUNT), 0) as "WAIVED_AMOUNT",
            FULFILLMENT_TYPE_DROPSHIP_CODE as "FULFILLMENT_TYPE_DROPSHIP_CODE",
            null as "REFUND_ADJUSTMENT_REASON_CODE",
            ''0'' as "RESTAURANT_TIP_AMOUNT",
            null as "RESTAURANT_LOYALTY_BENEFIT_TYPE",
            null as "RESTAURANT_DELIVERY_PARTNER",
            ''0'' as "PROMO_AMOUNT",
            null as "PROMO_BUSINESS_ORIGIN",
            null as "PARTNER_RELATIONSHIP_TYPE",
            null as "FUNDING_TYPE",
            ''0'' as "FUNDING_AMOUNT",
            null as "RESTAURANT_CONCEPT_NAME"
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
            and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26''
         group by
            SOURCE_REFERENCE_TYPE,
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
            TENDER_ACTIVITY_CODE,
            FEE_CODE_GL_STORE_FLAG,
            FEE_CODE_GL_STORE_NUMBER,
            CASH_DISBURSEMENT_LINE1,
            CASH_DISBURSEMENT_LINE2,
            WAIVED_REASON_CODE,
            FULFILLMENT_TYPE_DROPSHIP_CODE,
            SUBCLASS_GROUPING',
CURRENT_DATE + 1,
NULL,
'select
                              	COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
                              	COALESCE(sum(EMPLOYEE_DISCOUNT_AMOUNT), 0) +
                              	COALESCE(sum(TAX_AMOUNT), 0) +
                              	COALESCE(sum(TENDER_AMOUNT), 0) +
                              	COALESCE(sum(WAIVED_AMOUNT), 0) as "TOTAL_AMOUNTS"
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
                              	and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26'''
);

-- 3.3: JWN_SALES_MARKETPLACE (placeholder for new restaurant fields)
INSERT INTO public.aggregation_configuration
(file_name_prefix, file_delimiter, is_data_quotes_surrounded, aggregation_query, start_date, end_date, data_control_query)
VALUES (
'JWN_SALES_MARKETPLACE',
'|',
false,
'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
             	gen_random_uuid() as "AGGREGATION_ID",
             	SOURCE_REFERENCE_TYPE as "DATA_SOURCE",
             	TRANSACTION_DATE as "TRANSACTION_DATE",
             	SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
             	BUSINESS_DATE as "BUSINESS_DATE",
             	TRANSACTION_TYPE as "TRANSACTION_TYPE",
             	TRANSACTION_LINE_TYPE as "TRANSACTION_LINE_TYPE",
             	RINGING_STORE as "RINGING_STORE",
             	STORE_OF_INTENT as "STORE_OF_INTENT",
             	COALESCE(sum(LINE_ITEM_AMOUNT), 0) as "LINE_ITEM_AMOUNT",
             	''0'' as "EMPLOYEE_DISCOUNT_AMOUNT",
             	COALESCE(sum(TAX_AMOUNT), 0) as "TAX_AMOUNT",
             	COALESCE(sum(TENDER_AMOUNT), 0) as "TENDER_AMOUNT",
                null as "DEPT_ID",
             	null as "CLASS_ID",
             	FEE_CODE as "FEE_CODE",
             	TENDER_TYPE as "TENDER_TYPE",
             	null as "TENDER_CARD_TYPE_CODE",
             	null as "TENDER_CARD_SUBTYPE_CODE",
             	TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
             	COALESCE(sum(MARKETPLACE_JWN_COMMISSION_AMOUNT),0) as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
             	null as "TENDER_CAPTURE_TYPE" ,
             	null as "MID_MERCHANT_ID",
             	null as "TENDER_ACTIVITY_CODE",
             	null as "FEE_CODE_GL_STORE_FLAG",
             	null as "FEE_CODE_GL_STORE_NUMBER",
             	null as "CASH_DISBURSEMENT_LINE1",
             	null as "CASH_DISBURSEMENT_LINE2",
             	null as "WAIVED_REASON_CODE",
             	''0'' as "WAIVED_AMOUNT",
             	null as "FULFILLMENT_TYPE_DROPSHIP_CODE",
             	REFUND_ADJUSTMENT_REASON_CODE as "REFUND_ADJUSTMENT_REASON_CODE",
             	''0'' as "RESTAURANT_TIP_AMOUNT",
             	null as "RESTAURANT_LOYALTY_BENEFIT_TYPE",
             	null as "RESTAURANT_DELIVERY_PARTNER",
             	''0'' as "PROMO_AMOUNT",
             	null as "PROMO_BUSINESS_ORIGIN",
                ta.PARTNER_RELATIONSHIP_TYPE as "PARTNER_RELATIONSHIP_TYPE",
                null as "FUNDING_TYPE",
                ''0'' as "FUNDING_AMOUNT",
                null as "RESTAURANT_CONCEPT_NAME"
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
             	and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26''
             group by
                SOURCE_REFERENCE_TYPE,
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
                ta.PARTNER_RELATIONSHIP_TYPE',
CURRENT_DATE + 1,
NULL,
'select
                              	COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
                              	COALESCE(sum(TAX_AMOUNT), 0) +
                              	COALESCE(sum(TENDER_AMOUNT), 0) +
                              	COALESCE(sum(MARKETPLACE_JWN_COMMISSION_AMOUNT), 0) as "TOTAL_AMOUNTS"
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
                              	and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26'''
);

-- 3.4: JWN_SALES_PROMO (placeholder for new restaurant fields)
INSERT INTO public.aggregation_configuration
(file_name_prefix, file_delimiter, is_data_quotes_surrounded, aggregation_query, start_date, end_date, data_control_query)
VALUES (
'JWN_SALES_PROMO',
'|',
false,
'select STRING_AGG(cast(tld.transaction_line_id as TEXT), '','') as transaction_line_ids,
             	gen_random_uuid() as "AGGREGATION_ID",
             	SOURCE_REFERENCE_TYPE as "DATA_SOURCE",
             	TRANSACTION_DATE as "TRANSACTION_DATE",
             	SOURCE_PROCESSED_DATE as "SALES_SYSTEM_PROCESS_DATE",
             	BUSINESS_DATE as "BUSINESS_DATE",
             	TRANSACTION_TYPE as "TRANSACTION_TYPE",
             	TRANSACTION_LINE_TYPE as "TRANSACTION_LINE_TYPE",
             	RINGING_STORE as "RINGING_STORE",
             	STORE_OF_INTENT as "STORE_OF_INTENT",
             	''0'' as "LINE_ITEM_AMOUNT",
             	''0'' as "EMPLOYEE_DISCOUNT_AMOUNT",
             	''0'' as "TAX_AMOUNT",
             	''0'' as "TENDER_AMOUNT",
                null as "DEPT_ID",
             	null as "CLASS_ID",
             	null as "FEE_CODE",
             	null as "TENDER_TYPE",
             	null as "TENDER_CARD_TYPE_CODE",
             	null as "TENDER_CARD_SUBTYPE_CODE",
             	TRANSACTION_REVERSAL_CODE as "TRANSACTION_REVERSAL_INDICATOR",
             	''0'' as "MARKETPLACE_JWN_COMMISSION_AMOUNT",
             	null as "TENDER_CAPTURE_TYPE" ,
             	null as "MID_MERCHANT_ID",
             	null as "TENDER_ACTIVITY_CODE",
             	null as "FEE_CODE_GL_STORE_FLAG",
             	null as "FEE_CODE_GL_STORE_NUMBER",
             	null as "CASH_DISBURSEMENT_LINE1",
             	null as "CASH_DISBURSEMENT_LINE2",
             	null as "WAIVED_REASON_CODE",
             	''0'' as "WAIVED_AMOUNT",
             	null as "FULFILLMENT_TYPE_DROPSHIP_CODE",
             	null as "REFUND_ADJUSTMENT_REASON_CODE",
             	''0'' as "RESTAURANT_TIP_AMOUNT",
             	null as "RESTAURANT_LOYALTY_BENEFIT_TYPE",
             	null as "RESTAURANT_DELIVERY_PARTNER",
             	COALESCE(sum(PROMO_AMOUNT),0) as "PROMO_AMOUNT",
             	PROMO_BUSINESS_ORIGIN as "PROMO_BUSINESS_ORIGIN",
                null as "PARTNER_RELATIONSHIP_TYPE",
                null as "FUNDING_TYPE",
                ''0'' as "FUNDING_AMOUNT",
                null as "RESTAURANT_CONCEPT_NAME"
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
             	and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26''
             group by
                SOURCE_REFERENCE_TYPE,
             	TRANSACTION_DATE,
             	SOURCE_PROCESSED_DATE ,
             	BUSINESS_DATE,
             	TRANSACTION_TYPE,
             	TRANSACTION_LINE_TYPE,
             	RINGING_STORE,
             	STORE_OF_INTENT,
             	TRANSACTION_REVERSAL_CODE,
                PROMO_BUSINESS_ORIGIN',
CURRENT_DATE + 1,
NULL,
'select
                              	COALESCE(sum(PROMO_AMOUNT), 0) as "TOTAL_AMOUNTS"
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
                              	and ta.source_processed_date BETWEEN DATE ''2025-09-22'' AND DATE ''2025-09-26'''
);