-- Create vendor_distinct_records table
CREATE TABLE IF NOT EXISTS vendor_distinct_record (
    vendor_distinct_record_id SERIAL PRIMARY KEY,
    action VARCHAR(6) NOT NULL,
    vendor_number INT NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    kafka_topic VARCHAR(255) NOT NULL,
    kafka_partition INT NOT NULL,
    kafka_offset BIGINT NOT NULL,
    source_updated_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_vendor_action UNIQUE (vendor_number, action)
);
