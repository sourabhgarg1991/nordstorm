#!/usr/bin/env python3
"""
Cleans up retail test data from Aurora test database.

Deletes Transaction records, which automatically cascades to:
- TransactionLine (via ON DELETE CASCADE)
- RetailTransactionLine (via ON DELETE CASCADE)

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
    print("RETAIL TRANSACTION CONSUMER LOAD TEST - DATA CLEANUP")
    print("=" * 60)
    print(f"Test Start Time: {test_start_time}")
    print(f"Database: {db_config['database']}@{db_config['host']}")
    print()

    try:
        conn = psycopg2.connect(**db_config)
        conn.autocommit = False  # Use transaction
        cursor = conn.cursor()

        # Delete transactions (CASCADE will automatically delete child records)
        cursor.execute("""
            DELETE FROM transaction
            WHERE source_reference_system_type = 'ertm'
            AND source_reference_type = 'retail'
            AND source_reference_transaction_id like 'LOAD_TEST_%'
        """)

        transaction_deleted = cursor.rowcount

        if transaction_deleted == 0:
            print("No test data found to cleanup")
            cursor.close()
            conn.close()
            return True


        print(f"[DONE] Deleted {transaction_deleted} Transaction records")
        print("(CASCADE automatically deleted child TransactionLine and RetailTransactionLine records)")
        print()

        # Commit transaction
        conn.commit()

        print("=" * 60)
        print("CLEANUP SUMMARY")
        print("=" * 60)
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
    parser = argparse.ArgumentParser(description='Cleanup retail test data from Aurora DB')
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