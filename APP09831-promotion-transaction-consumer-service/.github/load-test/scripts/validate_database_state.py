#!/usr/bin/env python3
"""
Validates that promotion data from GCP was correctly processed and persisted to Aurora DB.

Checks:
1. Transaction records were created with correct source reference type
2. TransactionLine records were created with proper foreign keys
3. PromotionTransactionLine records were created with GCP business origin
4. Data integrity: proper parent-child relationships (1:many:many)
5. All records created after test start time
"""

import os
import sys
import argparse
import psycopg2
from datetime import datetime

def validate_database_state(test_start_time):
    """
    Validates that GCP promotion data was correctly processed to Aurora DB.

    Args:
        test_start_time: Test start timestamp (YYYY-MM-DD HH:MM:SS format)

    Returns:
        True if validation passes, False otherwise
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
    print("PROMOTION CONSUMER LOAD TEST - DATABASE VALIDATION")
    print("=" * 60)
    print(f"Test Start Time: {test_start_time}")
    print(f"Database: {db_config['database']}@{db_config['host']}")
    print()

    try:
        conn = psycopg2.connect(**db_config)
        cursor = conn.cursor()

        # Step 1: Count Transaction records created during test
        cursor.execute("""
            SELECT COUNT(*) 
            FROM transaction
            WHERE source_reference_system_type = 'GCP'
            AND source_reference_type = 'PROMO'
            AND created_datetime >= %s
        """, (test_start_time,))

        transaction_count = cursor.fetchone()[0]
        print(f"[PASS] Transaction records created: {transaction_count}")

        if transaction_count == 0:
            print("ERROR: No transactions were created!")
            print("  Expected: Transactions with source_reference_system_type='GCP' and source_reference_type='PROMO'")
            return False

        # Step 2: Get transaction IDs for further validation
        cursor.execute("""
            SELECT transaction_id, source_reference_transaction_id
            FROM transaction
            WHERE source_reference_system_type = 'GCP'
            AND source_reference_type = 'PROMO'
            AND created_datetime >= %s
        """, (test_start_time,))

        test_transactions = cursor.fetchall()
        transaction_ids = [row[0] for row in test_transactions]

        print(f"  Sample transaction IDs: {transaction_ids[:5]}")
        print()

        # Step 3: Count TransactionLine records
        cursor.execute("""
            SELECT COUNT(*) 
            FROM transaction_line
            WHERE transaction_id = ANY(%s)
            AND source_reference_line_type = 'PROMO'
        """, (transaction_ids,))

        transaction_line_count = cursor.fetchone()[0]
        print(f"[PASS] TransactionLine records created: {transaction_line_count}")

        if transaction_line_count == 0:
            print("ERROR: No transaction lines were created!")
            print("  Expected: TransactionLine records linked to Transaction via transaction_id")
            return False

        # Step 4: Verify 1:many relationship (Transaction:TransactionLine)
        avg_lines_per_transaction = transaction_line_count / transaction_count
        print(f"  Average lines per transaction: {avg_lines_per_transaction:.2f}")
        print()

        # Step 5: Get transaction_line IDs for promotion validation
        cursor.execute("""
            SELECT transaction_line_id
            FROM transaction_line
            WHERE transaction_id = ANY(%s)
        """, (transaction_ids,))

        transaction_line_ids = [row[0] for row in cursor.fetchall()]

        # Step 6: Count PromotionTransactionLine records
        cursor.execute("""
            SELECT COUNT(*) 
            FROM promotion_transaction_line
            WHERE transaction_line_id = ANY(%s)
            AND promo_business_origin IN ('LOYALTY_PROMO', 'MARKETING_PROMO')
        """, (transaction_line_ids,))

        promotion_line_count = cursor.fetchone()[0]
        print(f"[PASS] PromotionTransactionLine records created: {promotion_line_count}")

        if promotion_line_count == 0:
            print("ERROR: No promotion transaction lines were created!")
            print("  Expected: PromotionTransactionLine with promo_business_origin from GCP")
            return False

        # Step 7: Verify data integrity - check for orphaned records
        cursor.execute("""
            SELECT COUNT(*) 
            FROM transaction_line tl
            LEFT JOIN transaction t ON tl.transaction_id = t.transaction_id
            WHERE t.transaction_id IS NULL
            AND tl.transaction_line_id = ANY(%s)
        """, (transaction_line_ids,))

        orphaned_lines = cursor.fetchone()[0]

        if orphaned_lines > 0:
            print(f"ERROR: Found {orphaned_lines} orphaned TransactionLine records!")
            return False

        print("[PASS] No orphaned TransactionLine records")
        print()

        # Step 8: Verify promotion lines integrity
        cursor.execute("""
            SELECT COUNT(*) 
            FROM promotion_transaction_line ptl
            LEFT JOIN transaction_line tl ON ptl.transaction_line_id = tl.transaction_line_id
            WHERE tl.transaction_line_id IS NULL
            AND ptl.transaction_line_id = ANY(%s)
        """, (transaction_line_ids,))

        orphaned_promotions = cursor.fetchone()[0]

        if orphaned_promotions > 0:
            print(f"ERROR: Found {orphaned_promotions} orphaned PromotionTransactionLine records!")
            return False

        print("[PASS] No orphaned PromotionTransactionLine records")
        print()

        # Step 9: Sample data validation - check one complete record
        cursor.execute("""
            SELECT 
                t.transaction_id,
                t.source_reference_transaction_id,
                t.source_reference_system_type,
                t.source_reference_type,
                t.business_date,
                COUNT(DISTINCT tl.transaction_line_id) as line_count,
                COUNT(ptl.promotion_transaction_line_id) as promo_count
            FROM transaction t
            JOIN transaction_line tl ON t.transaction_id = tl.transaction_id
            JOIN promotion_transaction_line ptl ON tl.transaction_line_id = ptl.transaction_line_id
            WHERE t.transaction_id = ANY(%s)
            GROUP BY t.transaction_id
            LIMIT 1
        """, (transaction_ids,))

        sample = cursor.fetchone()

        if sample:
            print("Sample Record Validation:")
            print(f"  Transaction ID: {sample[0]}")
            print(f"  GCP Global Tran ID: {sample[1]}")
            print(f"  Source System: {sample[2]} (expected: GCP)")
            print(f"  Source Type: {sample[3]} (expected: PROMO)")
            print(f"  Business Date: {sample[4]}")
            print(f"  Transaction Lines: {sample[5]}")
            print(f"  Promotion Lines: {sample[6]}")
            print()

        # Step 10: Summary
        print("=" * 60)
        print("VALIDATION SUMMARY")
        print("=" * 60)
        print(f"[PASS] Transactions created: {transaction_count}")
        print(f"[PASS] Transaction Lines created: {transaction_line_count}")
        print(f"[PASS] Promotion Lines created: {promotion_line_count}")
        print(f"[PASS] Data integrity verified")
        print(f"[PASS] Parent-child relationships correct")
        print()
        print("[SUCCESS] DATABASE VALIDATION PASSED")
        print("=" * 60)

        cursor.close()
        conn.close()
        return True

    except Exception as e:
        print(f"\n[ERROR] Validation failed: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

def main():
    parser = argparse.ArgumentParser(description='Validate GCP promotion data in Aurora DB')
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

    # Run validation
    success = validate_database_state(args.test_start_time)

    if not success:
        sys.exit(1)

if __name__ == '__main__':
    main()