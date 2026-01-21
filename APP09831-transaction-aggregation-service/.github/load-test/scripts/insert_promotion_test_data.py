#!/usr/bin/env python3
"""
Insert test data with unique identifier for easy validation and cleanup.
"""

import argparse
import json
import os
import sys
from datetime import date
import psycopg2


# Unique test identifier - use a store that doesn't exist in production
TEST_STORE = '9999'
TEST_PROMO_ORIGIN = 'LOAD_TEST'

# Date range from aggregation query
DATE_START = date(2025, 9, 22)
DATE_END = date(2025, 9, 26)


def insert_test_data(count: int):
    conn = psycopg2.connect(
        host=os.environ['DB_HOST'],
        database=os.environ['DB_NAME'],
        user=os.environ['DB_USER'],
        password=os.environ['DB_PASSWORD'],
        port=5432
    )
    cursor = conn.cursor()
    conn.autocommit = False

    inserted_ids = {
        'transactions': [],
        'transaction_lines': [],
        'test_store': TEST_STORE,
        'test_promo_origin': TEST_PROMO_ORIGIN
    }

    try:
        print(f"Inserting {count} test transactions with store_of_intent={TEST_STORE}")

        for i in range(count):
            # Insert transaction
            cursor.execute("""
                INSERT INTO transaction (
                    source_reference_transaction_id, source_reference_system_type,
                    source_reference_type, source_processed_date, transaction_date,
                    business_date, transaction_type, transaction_reversal_code
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                RETURNING transaction_id
            """, (
                f'LOADTEST_{os.environ.get("GITHUB_RUN_ID", "local")}_{i}',
                'GCP', 'PROMO', DATE_START, DATE_START, DATE_START, 'SALE', 'N'
            ))

            transaction_id = cursor.fetchone()[0]
            inserted_ids['transactions'].append(transaction_id)

            # Insert transaction_line with TEST_STORE
            cursor.execute("""
                INSERT INTO transaction_line (
                    transaction_id, source_reference_line_id, source_reference_line_type,
                    transaction_line_type, store_of_intent
                ) VALUES (%s, %s, %s, %s, %s)
                RETURNING transaction_line_id
            """, (transaction_id, f'{i}', 'PROMO', 'SALE', TEST_STORE))

            transaction_line_id = cursor.fetchone()[0]
            inserted_ids['transaction_lines'].append(transaction_line_id)

            # Insert promotion with TEST_PROMO_ORIGIN
            cursor.execute("""
                INSERT INTO promotion_transaction_line (
                    transaction_line_id, promo_amount, promo_business_origin
                ) VALUES (%s, %s, %s)
            """, (transaction_line_id, 10.00, TEST_PROMO_ORIGIN))

        conn.commit()
        print(f"Inserted {count} transactions successfully")

    except Exception as e:
        conn.rollback()
        print(f"Error: {e}")
        raise
    finally:
        cursor.close()
        conn.close()

    return inserted_ids


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--count', type=int, required=True)
    parser.add_argument('--output-file', type=str, required=True)
    args = parser.parse_args()

    try:
        inserted_ids = insert_test_data(args.count)

        with open(args.output_file, 'w') as f:
            json.dump(inserted_ids, f, indent=2)

        print(f"Metadata saved to {args.output_file}")

    except Exception as e:
        print(f"Failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()

