ALTER TABLE "aggregation_configuration" RENAME COLUMN QUERY TO aggregation_query;

ALTER TABLE "aggregation_configuration" ADD COLUMN IF NOT EXISTS DATA_CONTROL_QUERY text;

UPDATE public.aggregation_configuration
    set DATA_CONTROL_QUERY = 'select
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
                              	and ta.source_processed_date < date(localtimestamp)',
    last_updated_datetime=CURRENT_TIMESTAMP
    where file_name_prefix = 'Retail_Transaction_Aggregation';

UPDATE public.aggregation_configuration
    set DATA_CONTROL_QUERY = 'select
                              	COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
                              	COALESCE(sum(EMPLOYEE_DISCOUNT_AMOUNT), 0)  +
                              	COALESCE(sum(TAX_AMOUNT), 0) +
                              	COALESCE(sum(TENDER_AMOUNT), 0)  +
                              	COALESCE(sum(RESTAURANT_TIP_AMOUNT), 0)  as "TOTAL_AMOUNTS"
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
                              	and ta.source_processed_date < date(localtimestamp)',
    last_updated_datetime=CURRENT_TIMESTAMP
    where file_name_prefix = 'Restaurant_Transaction_Aggregation';

UPDATE public.aggregation_configuration
    set DATA_CONTROL_QUERY = 'select
                              	COALESCE(sum(LINE_ITEM_AMOUNT), 0) +
                              	COALESCE(sum(TAX_AMOUNT), 0)  +
                              	COALESCE(sum(TENDER_AMOUNT), 0)  +
                              	COALESCE(sum(MARKETPLACE_JWN_COMMISSION_AMOUNT), 0)  as "TOTAL_AMOUNTS"
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
                              	and ta.source_processed_date < date(localtimestamp)',
    last_updated_datetime=CURRENT_TIMESTAMP
    where file_name_prefix = 'Marketplace_Transaction_Aggregation';


UPDATE public.aggregation_configuration
    set DATA_CONTROL_QUERY = 'select
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
                              	and ta.source_processed_date < date(localtimestamp)',
    last_updated_datetime=CURRENT_TIMESTAMP
    where file_name_prefix = 'Promotion_Transaction_Aggregation';