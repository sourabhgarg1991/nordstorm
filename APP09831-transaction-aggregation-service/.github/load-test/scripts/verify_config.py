#!/usr/bin/env python3
"""
Verify that required aggregation configuration exists before running load test.
"""

import argparse
import os
import sys
import traceback
from datetime import date
import psycopg2
from psycopg2.extras import RealDictCursor


def verify_aggregation_config(config_prefix: str):
    """
    Verify that an active aggregation configuration exists for the given prefix.

    Args:
        config_prefix: File name prefix to search for (e.g., 'Retail', 'Restaurant', 'Marketplace', 'Promotion')

    Returns:
        dict: Configuration details if found

    Raises:
        SystemExit: If configuration not found or invalid
    """
    conn = psycopg2.connect(
        host=os.environ['DB_HOST'],
        database=os.environ['DB_NAME'],
        user=os.environ['DB_USER'],
        password=os.environ['DB_PASSWORD'],
        port=5432,
        cursor_factory=RealDictCursor
    )
    cursor = conn.cursor()

    try:
        # Search for active configuration (only valid load test configurations)
        today = date.today()
        today_str = today.strftime('%Y-%m-%d')

        search_pattern = f'%{config_prefix}%'
        cursor.execute("""
            SELECT
                aggregation_configuration_id,
                file_name_prefix,
                file_delimiter,
                is_data_quotes_surrounded,
                start_date,
                end_date,
                aggregation_query,
                data_control_query
            FROM aggregation_configuration
            WHERE UPPER(file_name_prefix) LIKE UPPER(%s)
              AND start_date <= %s
              AND (end_date IS NULL OR end_date >= %s)
              AND file_name_prefix NOT LIKE '%%_Integration_Test'
              AND (
                file_name_prefix = 'JWN_SALES_RETAIL'
                OR file_name_prefix = 'JWN_SALES_RESTAURANT'
                OR file_name_prefix = 'JWN_SALES_MARKETPLACE'
                OR file_name_prefix = 'JWN_SALES_PROMO'
              )
            ORDER BY created_datetime DESC
            LIMIT 1
        """, (search_pattern, today_str, today_str))

        config = cursor.fetchone()

        # Valid load test configuration prefixes (excludes test configs like Empty_Result_Test)
        VALID_PREFIXES = [
            'JWN_SALES_RETAIL',
            'JWN_SALES_RESTAURANT',
            'JWN_SALES_MARKETPLACE',
            'JWN_SALES_PROMO'
        ]

        # If config found, verify it's one of the valid load test configurations
        if config:
            found_prefix = config['file_name_prefix']
            is_valid = any(found_prefix.startswith(valid_prefix) for valid_prefix in VALID_PREFIXES)

            if not is_valid:
                print(f"WARNING: Configuration '{found_prefix}' is not a valid load test configuration")
                print(f"Valid configurations: {', '.join(VALID_PREFIXES)}")
                print(f"Skipping this configuration and looking for valid one...")
                config = None  # Treat as not found

        if not config:
            print(f"ERROR: No active aggregation configuration found for '{config_prefix}'")
            print(f"Search criteria:")
            print(f"  - File name prefix contains: {config_prefix}")
            print(f"  - Must be one of: {', '.join(VALID_PREFIXES)}")
            print(f"  - Start date <= {today}")
            print(f"  - End date IS NULL OR >= {today}")
            print("")

            # Show all available configurations
            cursor.execute("""
                SELECT file_name_prefix, start_date, end_date,
                       aggregation_configuration_id
                FROM aggregation_configuration
                ORDER BY created_datetime DESC
            """)

            all_configs = cursor.fetchall()
            if all_configs:
                print("Available configurations:")
                for cfg in all_configs:
                    # Compare dates properly - cfg values are date objects from DB
                    cfg_start = cfg['start_date']
                    cfg_end = cfg['end_date']
                    status = "ACTIVE" if (
                        cfg_start <= today and
                        (cfg_end is None or cfg_end >= today)
                    ) else "INACTIVE"

                    # Mark if it's a valid load test config
                    is_valid_cfg = any(cfg['file_name_prefix'].startswith(vp) for vp in VALID_PREFIXES)
                    validity = "✓ Valid" if is_valid_cfg else "✗ Test Config (Ignored)"

                    print(f"  - {cfg['file_name_prefix']} "
                          f"(ID: {cfg['aggregation_configuration_id']}, "
                          f"Start: {cfg['start_date']}, "
                          f"End: {cfg['end_date']}, "
                          f"Status: {status}, {validity})")
            else:
                print("No configurations found in database!")

            sys.exit(1)

        # Configuration found - display details
        print(f"✓ Found active aggregation configuration:")
        print(f"  Configuration ID: {config['aggregation_configuration_id']}")
        print(f"  File Prefix: {config['file_name_prefix']}")
        print(f"  Delimiter: '{config['file_delimiter']}'")
        print(f"  Quotes Surrounded: {config['is_data_quotes_surrounded']}")
        print(f"  Start Date: {config['start_date']}")
        print(f"  End Date: {config['end_date'] or 'No end date (active indefinitely)'}")
        print(f"  Has Aggregation Query: {bool(config['aggregation_query'])}")
        print(f"  Has Control Query: {bool(config['data_control_query'])}")
        print("")

        # Validate required fields
        if not config['aggregation_query']:
            print("ERROR: Configuration has no aggregation query!")
            sys.exit(1)

        return dict(config)

    finally:
        cursor.close()
        conn.close()


def main():
    parser = argparse.ArgumentParser(
        description='Verify aggregation configuration exists before load test'
    )
    parser.add_argument(
        '--config-prefix',
        type=str,
        required=True,
        help='File name prefix to search for (e.g., Retail, Restaurant, Marketplace, Promotion)'
    )
    args = parser.parse_args()

    try:
        config = verify_aggregation_config(args.config_prefix)
        print(f"Configuration verification passed ✓")

    except Exception as e:
        print(f"Configuration verification failed: {e}")
        print("\nFull error details:")
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()

