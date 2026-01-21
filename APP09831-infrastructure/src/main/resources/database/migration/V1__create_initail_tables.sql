create table if not exists "aggregation_configuration" (
    "aggregation_configuration_id" bigserial primary key not null,
    "file_name_prefix" varchar(100) not null,
    "file_delimiter" char(1) not null,
    "is_data_quotes_surrounded" boolean not null,
    "query" text not null,
    "start_date" date not null,
    "end_date" date,
    "created_datetime" timestamp default current_timestamp,
    "last_updated_datetime" timestamp default current_timestamp
);

create table if not exists "generated_file_detail" (
    "generated_file_detail_id" bigserial primary key,
    "aggregation_configuration_id" bigint not null references "aggregation_configuration" ("aggregation_configuration_id"),
    "generated_file_name" varchar(500) not null,
    "file_content" text not null,
    "created_datetime" timestamp default current_timestamp,
    "last_updated_datetime" timestamp default current_timestamp
);

create table if not exists "filtered_transaction" (
    "filtered_transaction_id" bigserial primary key,
    "source_reference_transaction_id" varchar(100) not null,
    "source_reference_system_type" varchar(50) not null,
    "source_reference_type" varchar(50) not null,
    "filtered_reason" varchar(200) not null,
    "is_published" boolean not null default false,
    "created_datetime" timestamp default current_timestamp,
    "last_updated_datetime" timestamp default current_timestamp
);
create index if not exists idx_filtered_transaction_is_published
    on "filtered_transaction" ("is_published");

create table if not exists "transaction" (
    "transaction_id" bigserial primary key,
    "source_reference_transaction_id" varchar(100) not null,
    "source_reference_system_type" varchar(50) not null,
    "line_of_business" varchar(50) not null,
    "source_processed_date" date not null,
    "transaction_date" date not null,
    "business_date" date not null,
    "transaction_type" varchar(50) not null,
    "transaction_reversal_code" char(1) not null,
    "partner_relationship_type" varchar(100),
    "created_datetime" timestamp default current_timestamp,
    "last_updated_datetime" timestamp default current_timestamp
);
create index if not exists idx_transaction_src_ref_txn_id
    on "transaction" ("source_reference_transaction_id");
create index if not exists idx_transaction_src_ref_sys_type
    on "transaction" ("source_reference_system_type");
create index if not exists idx_transaction_line_of_business
    on "transaction" ("line_of_business");

create table if not exists "transaction_line" (
    "transaction_line_id" bigserial primary key,
    "transaction_id" bigint not null,
    "source_reference_line_id" varchar(100) not null,
    "source_reference_line_type" varchar(100) not null,
    "transaction_line_type" varchar(50),
    "ringing_store" varchar(10),
    "store_of_intent" varchar(10),
    foreign key ("transaction_id") references "transaction" ("transaction_id") on delete cascade
);
create index if not exists idx_transaction_line_transaction_id
    on "transaction_line" ("transaction_id");
create index if not exists idx_transaction_line_source_reference_line_id
    on "transaction_line" ("source_reference_line_id");

create table if not exists "retail_transaction_line" (
    "retail_transaction_line_id" bigserial primary key,
    "transaction_line_id" bigint not null,
    "department_id" varchar(100),
    "class_id" varchar(100),
    "fee_code" varchar(100),
    "tender_type" varchar(100),
    "tender_card_type_code" varchar(100),
    "tender_card_subtype_code" varchar(100),
    "tender_capture_type" varchar(100),
    "tender_adjustment_code" varchar(100),
    "line_item_amount" decimal,
    "tax_amount" decimal,
    "employee_discount_amount" decimal,
    "tender_amount" decimal,
    "mid_merchant_id" varchar(100),
    "fee_code_gl_store_flag" varchar(100),
    "fee_code_gl_store_number" varchar(100),
    "fulfillment_type_dropship_code" varchar(100),
    "cash_disbursement_line1" varchar(100),
    "cash_disbursement_line2" varchar(100),
    "waived_reason_code" varchar(100),
    "waived_amount" decimal,
    "subclass_grouping" varchar(100),
    foreign key ("transaction_line_id") references "transaction_line" ("transaction_line_id") on delete cascade
);
create index if not exists idx_retail_transaction_line_transaction_line_id
    on "retail_transaction_line" ("transaction_line_id");

create table if not exists "restaurant_transaction_line" (
    "restaurant_transaction_line_id" bigserial primary key,
    "transaction_line_id" bigint not null,
    "line_item_amount" decimal,
    "employee_discount_amount" decimal,
    "tax_amount" decimal,
    "tender_amount" decimal,
    "restaurant_tip_amount" decimal,
    "department_id" varchar(100),
    "class_id" varchar(100),
    "tender_type" varchar(100),
    "tender_card_type_code" varchar(100),
    "tender_card_subtype_code" varchar(100),
    "tender_capture_type" varchar(100),
    "restaurant_loyalty_benefit_type" varchar(100),
    "restaurant_delivery_partner" varchar(100),
    foreign key ("transaction_line_id") references "transaction_line" ("transaction_line_id") on delete cascade
);
create index if not exists idx_restaurant_transaction_line_transaction_line_id
    on "restaurant_transaction_line" ("transaction_line_id");

create table if not exists "marketplace_transaction_line" (
    "marketplace_transaction_line_id" bigserial primary key,
    "transaction_line_id" bigint not null,
    "partner_relationship_type" varchar(100),
    "line_item_amount" decimal,
    "tax_amount" decimal,
    "tender_amount" decimal,
    "marketplace_jwn_commission_amount" decimal,
    "fee_code" varchar(100),
    "tender_type" varchar(100),
    "refund_adjustment_reason_code" varchar(100),
    foreign key ("transaction_line_id") references "transaction_line" ("transaction_line_id") on delete cascade
);
create index if not exists idx_marketplace_transaction_line_transaction_line_id
    on "marketplace_transaction_line" ("transaction_line_id");

create table if not exists "promotion_transaction_line" (
    "promotion_transaction_line_id" bigserial primary key,
    "transaction_line_id" bigint not null,
    "promo_type" varchar(100),
    "promo_amount" decimal,
    "promo_business_origin" varchar(100),
    foreign key ("transaction_line_id") references "transaction_line" ("transaction_line_id") on delete cascade
);
create index if not exists idx_promotion_transaction_line_transaction_line_id
    on "promotion_transaction_line" ("transaction_line_id");

create table if not exists "transaction_aggregation_relation" (
    "transaction_aggregation_relation_id" bigserial primary key,
    "aggregation_id" uuid not null,
    "transaction_line_id" bigint not null,
    "is_published" boolean not null default false,
    "created_datetime" timestamp default current_timestamp,
    "last_updated_datetime" timestamp default current_timestamp,
    foreign key ("transaction_line_id") references "transaction_line" ("transaction_line_id") on delete cascade
);
create index if not exists idx_transaction_aggregation_relation_aggregation_id
    on "transaction_aggregation_relation" ("aggregation_id");
create index if not exists idx_transaction_aggregation_relation_transaction_line_id
    on "transaction_aggregation_relation" ("transaction_line_id");
create index if not exists idx_transaction_aggregation_relation_is_posted_to_kafka
    on "transaction_aggregation_relation" ("is_published");