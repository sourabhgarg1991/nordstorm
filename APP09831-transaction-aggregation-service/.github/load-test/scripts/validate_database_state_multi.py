#!/usr/bin/env python3
"""
Validate aggregation results for promotion test data.
"""

import argparse
import json
import os
import sys
import psycopg2
from psycopg2.extras import RealDictCursor


def validate(test_data_file: str, expected_file_pattern: str = 'PROMO'):
    with open(test_data_file, 'r') as f:
        test_data = json.load(f)

    transaction_line_ids = test_data['transaction_lines']

    conn = psycopg2.connect(
        host=os.environ['DB_HOST'],
        database=os.environ['DB_NAME'],
        user=os.environ['DB_USER'],
        password=os.environ['DB_PASSWORD'],
        port=5432,
        cursor_factory=RealDictCursor
    )
    cursor = conn.cursor()

    print(f"Validating test data for {len(transaction_line_ids)} transaction lines")

    # Check transaction_aggregation_relation entries for our transaction_line_ids
    cursor.execute("""
        SELECT aggregation_id, COUNT(*) as line_count
        FROM transaction_aggregation_relation
        WHERE transaction_line_id = ANY(%s)
        GROUP BY aggregation_id
    """, (transaction_line_ids,))

    results = cursor.fetchall()

    if not results:
        print("ERROR: No aggregation relations found")
        cursor.close()
        conn.close()
        sys.exit(1)

    # Should have 1 aggregation_id because all our test data groups together
    print(f"Found {len(results)} aggregation group(s)")
    for r in results:
        print(f"  Aggregation ID: {r['aggregation_id']} ({r['line_count']} lines)")

    # Check generated PROMO file
    cursor.execute("""
        SELECT generated_file_name, is_uploaded_to_s3, created_datetime
        FROM generated_file_detail
        WHERE UPPER(generated_file_name) LIKE %s
        ORDER BY created_datetime DESC
        LIMIT 1
    """, (f'%{expected_file_pattern}%',))

    result = cursor.fetchone()

    if not result:
        print(f"ERROR: No {expected_file_pattern} file found")
        cursor.close()
        conn.close()
        sys.exit(1)

    print(f"Generated file: {result['generated_file_name']}")
    print(f"Uploaded to S3: {result['is_uploaded_to_s3']}")

    if not result['is_uploaded_to_s3']:
        print("ERROR: File not uploaded to S3")
        cursor.close()
        conn.close()
        sys.exit(1)

    cursor.close()
    conn.close()

    print("Validation passed")


def main():
    parser = argparse.ArgumentParser(
        description='Validate promotion aggregation results'
    )
    parser.add_argument('--test-data-file', type=str, required=True)
    parser.add_argument('--file-pattern', type=str, required=False,
                        default='PROMO',
                        help='Expected file pattern (default: PROMO)')
    args = parser.parse_args()

    try:
        validate(args.test_data_file, args.file_pattern)
    except Exception as e:
        print(f"Validation failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()

