ALTER SEQUENCE public.transaction_transaction_id_seq cache 2000;
ALTER SEQUENCE public.transaction_line_transaction_line_id_seq cache 2000;
ALTER SEQUENCE public.retail_transaction_line_retail_transaction_line_id_seq cache 2000;
ALTER SEQUENCE public.restaurant_transaction_line_restaurant_transaction_line_id_seq cache 2000;
ALTER SEQUENCE public.promotion_transaction_line_promotion_transaction_line_id_seq cache 2000;
ALTER SEQUENCE public.marketplace_transaction_line_marketplace_transaction_line_i_seq cache 2000;
ALTER SEQUENCE public.transaction_aggregation_relat_transaction_aggregation_relat_seq cache 2000;


UPDATE public.aggregation_configuration set file_name_prefix='JWN_SALES_RETAIL', last_updated_datetime=CURRENT_TIMESTAMP
      WHERE file_name_prefix = 'RETAIL';

UPDATE public.aggregation_configuration set file_name_prefix='JWN_SALES_RESTAURANT', last_updated_datetime=CURRENT_TIMESTAMP
      WHERE file_name_prefix = 'RESTAURANT';

UPDATE public.aggregation_configuration set file_name_prefix='JWN_SALES_MARKETPLACE', last_updated_datetime=CURRENT_TIMESTAMP
      WHERE file_name_prefix = 'MARKETPLACE';

UPDATE public.aggregation_configuration set file_name_prefix='JWN_SALES_PROMO', last_updated_datetime=CURRENT_TIMESTAMP
      WHERE file_name_prefix = 'PROMOTION';