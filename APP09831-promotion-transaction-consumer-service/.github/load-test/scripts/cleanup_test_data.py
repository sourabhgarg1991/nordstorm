#!/usr/bin/env python3
"""
Cleans up promotion test data from Aurora test database.

Deletes all records created during the load test in correct order:
1. PromotionTransactionLine (child)
2. TransactionLine (child)
3. Transaction (parent)

Uses test start time to identify records created during load test.
"""

import os
import sys
import argparse
import psycopg2

def cleanup_test_data(test_start_time):
    """
    Deletes all test data created during the load test.

    Args:
        test_start_time: Test start timestamp (YYYY-MM-DD HH:MM:SS format)

    Returns:
        True if cleanup succeeds, False otherwise
    """
    # Database connection parameters
    db_config = {
        'host': os.environ['DB_HOST'],
        'database': os.environ['DB_NAME'],
        'user': os.environ['DB_USER'],
        'password': os.environ['DB_PASSWORD'],
        'port': 5432
    }

    print("=" * 60)
    print("PROMOTION CONSUMER LOAD TEST - DATA CLEANUP")
    print("=" * 60)
    print(f"Test Start Time: {test_start_time}")
    print(f"Database: {db_config['database']}@{db_config['host']}")
    print()

    try:
        conn = psycopg2.connect(**db_config)
        conn.autocommit = False  # Use transaction
        cursor = conn.cursor()

        # Step 1: Find transaction IDs to delete
        cursor.execute("""
            SELECT transaction_id, source_reference_transaction_id
            FROM transaction
            WHERE source_reference_system_type = 'GCP'
            AND source_reference_type = 'PROMO'
            AND created_datetime >= %s
        """, (test_start_time,))

        test_transactions = cursor.fetchall()
        transaction_ids = [row[0] for row in test_transactions]

        if not transaction_ids:
            print("No test data found to cleanup")
            cursor.close()
            conn.close()
            return True

        print(f"Found {len(transaction_ids)} transactions to delete")
        print(f"Sample transaction IDs: {transaction_ids[:5]}")
        print()

        # Step 2: Get transaction_line IDs
        cursor.execute("""
            SELECT transaction_line_id
            FROM transaction_line
            WHERE transaction_id = ANY(%s)
        """, (transaction_ids,))

        transaction_line_ids = [row[0] for row in cursor.fetchall()]
        print(f"Found {len(transaction_line_ids)} transaction lines to delete")
        print()

        # Step 3: Delete PromotionTransactionLine records (child)
        cursor.execute("""
            DELETE FROM promotion_transaction_line
            WHERE transaction_line_id = ANY(%s)
        """, (transaction_line_ids,))

        promotion_deleted = cursor.rowcount
        print(f"[DONE] Deleted {promotion_deleted} PromotionTransactionLine records")

        # Step 4: Delete TransactionLine records (child)
        cursor.execute("""
            DELETE FROM transaction_line
            WHERE transaction_id = ANY(%s)
        """, (transaction_ids,))

        line_deleted = cursor.rowcount
        print(f"[DONE] Deleted {line_deleted} TransactionLine records")

        # Step 5: Delete Transaction records (parent)
        cursor.execute("""
            DELETE FROM transaction
            WHERE transaction_id = ANY(%s)
        """, (transaction_ids,))

        transaction_deleted = cursor.rowcount
        print(f"[DONE] Deleted {transaction_deleted} Transaction records")
        print()

        # Commit transaction
        conn.commit()

        print("=" * 60)
        print("CLEANUP SUMMARY")
        print("=" * 60)
        print(f"[DONE] Promotion Lines deleted: {promotion_deleted}")
        print(f"[DONE] Transaction Lines deleted: {line_deleted}")
        print(f"[DONE] Transactions deleted: {transaction_deleted}")
        print()
        print("[SUCCESS] CLEANUP COMPLETED SUCCESSFULLY")
        print("=" * 60)

        cursor.close()
        conn.close()
        return True

    except Exception as e:
        print(f"\n[ERROR] ERROR during cleanup: {str(e)}")

        # Rollback on error
        if conn:
            conn.rollback()
            print("Transaction rolled back")

        import traceback
        traceback.print_exc()
        return False

def main():
    parser = argparse.ArgumentParser(description='Cleanup promotion test data from Aurora DB')
    parser.add_argument('--test-start-time',
                        required=True,
                        help='Test start time in YYYY-MM-DD HH:MM:SS format')

    args = parser.parse_args()

    # Validate environment variables
    required_env_vars = ['DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASSWORD']
    missing_vars = [var for var in required_env_vars if var not in os.environ]

    if missing_vars:
        print(f"ERROR: Missing required environment variables: {', '.join(missing_vars)}")
        sys.exit(1)

    # Run cleanup
    success = cleanup_test_data(args.test_start_time)

    if not success:
        sys.exit(1)

if __name__ == '__main__':
    main()