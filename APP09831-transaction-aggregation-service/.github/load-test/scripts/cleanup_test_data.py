#!/usr/bin/env python3
"""
Cleanup test data after load test for all transaction types.
Uses CASCADE DELETE - all foreign keys have CASCADE configured, so deleting parent
transaction table automatically deletes all children:
- transaction_line
- retail/restaurant/marketplace/promotion_transaction_line
- transaction_aggregation_relation

Supports: retail, restaurant, marketplace, promotion
"""

import argparse
import json
import os
import sys
import traceback
import psycopg2


def cleanup(test_data_file: str):
    with open(test_data_file, 'r') as f:
        test_data = json.load(f)

    transaction_ids = test_data['transactions']

    conn = psycopg2.connect(
        host=os.environ['DB_HOST'],
        database=os.environ['DB_NAME'],
        user=os.environ['DB_USER'],
        password=os.environ['DB_PASSWORD'],
        port=5432
    )
    cursor = conn.cursor()
    conn.autocommit = False

    try:
        print(f"Cleaning up {len(transaction_ids)} transactions (using CASCADE DELETE)")

        # Step 1: Delete generated files FIRST
        # NOTE: generated_file_detail is NOT in the CASCADE DELETE chain because it references
        # aggregation_configuration (not transaction/transaction_line).
        # We must delete it manually based on file name pattern and creation time.
        cursor.execute("""
            DELETE FROM generated_file_detail
            WHERE generated_file_detail_id IN (
                SELECT gfd.generated_file_detail_id
                FROM generated_file_detail gfd
                WHERE gfd.created_datetime >= CURRENT_DATE - INTERVAL '1 hour'
                AND (
                    UPPER(gfd.generated_file_name) LIKE '%PROMO%'
                    OR UPPER(gfd.generated_file_name) LIKE '%RETAIL%'
                    OR UPPER(gfd.generated_file_name) LIKE '%RESTAURANT%'
                    OR UPPER(gfd.generated_file_name) LIKE '%MARKETPLACE%'
                )
                ORDER BY gfd.created_datetime DESC
                LIMIT 10
            )
        """)
        print(f"Deleted {cursor.rowcount} generated file(s)")

        # Step 2: Delete parent transaction table
        # CASCADE DELETE will automatically delete all children in the transaction hierarchy:
        # - transaction_line (FK with CASCADE)
        #   - retail_transaction_line (FK with CASCADE)
        #   - restaurant_transaction_line (FK with CASCADE)
        #   - marketplace_transaction_line (FK with CASCADE)
        #   - promotion_transaction_line (FK with CASCADE)
        #   - transaction_aggregation_relation (FK with CASCADE)
        #
        # SAFETY CHECK: Only delete transactions with LOADTEST prefix to prevent accidental deletion of real data
        if transaction_ids and len(transaction_ids) > 0:
            placeholders = ','.join(['%s'] * len(transaction_ids))
            query = f"""
                DELETE FROM transaction
                WHERE transaction_id IN ({placeholders})
                AND source_reference_transaction_id LIKE %s
            """
            # Add LOADTEST pattern as the last parameter for safety check
            cursor.execute(query, (*transaction_ids, 'LOADTEST%'))
            deleted_count = cursor.rowcount
            print(f"Deleted {deleted_count} transactions (with LOADTEST prefix)")

            # Warn if expected count doesn't match (might indicate wrong IDs or non-test data)
            if deleted_count != len(transaction_ids):
                print(f" Warning: Expected to delete {len(transaction_ids)} but deleted {deleted_count}")
                print(f" Some transactions may not have LOADTEST prefix")

            if deleted_count > 0:
                print(f" → CASCADE automatically deleted all child records:")
                print(f" - transaction_line")
                print(f" - retail/restaurant/marketplace/promotion_transaction_line")
                print(f" - transaction_aggregation_relation")
        else:
            print("No transactions to delete (transaction_ids is empty)")

        conn.commit()
        print("Cleanup completed successfully")

    except Exception as e:
        conn.rollback()
        print(f"Cleanup failed: {e}")
        print(f"Error type: {type(e).__name__}")
        print(f"Traceback:")
        traceback.print_exc()
        raise
    finally:
        cursor.close()
        conn.close()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-data-file', type=str, required=True)
    args = parser.parse_args()

    try:
        cleanup(args.test_data_file)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()
