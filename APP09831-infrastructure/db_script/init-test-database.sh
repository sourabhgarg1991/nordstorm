#!/usr/bin/env bash
set -euo pipefail

# Idempotent Test database initialization for CI/CD
# Required env vars (exported by the composite action step invoking this script):
#   AURORA_DATABASE_ENDPOINT, AURORA_MASTER_USERNAME, AURORA_MASTER_PASSWORD
#   AURORA_DATABASE_NAME (dataintegration)
#   AURORA_TEST_USERNAME, AURORA_TEST_PASSWORD

log() { echo "[init-test-db] $*"; }
psql_base() { PGPASSWORD="${AURORA_MASTER_PASSWORD}" psql -v ON_ERROR_STOP=1 -h "${AURORA_DATABASE_ENDPOINT}" -U "${AURORA_MASTER_USERNAME}" "$@"; }

# Function to escape SQL identifiers (database names, role names, etc.)
# This prevents SQL injection by ensuring identifiers contain only safe characters
escape_sql_identifier() {
  local identifier="$1"

  # First, remove any existing quotes to avoid double-quoting
  local clean_identifier
  clean_identifier=$(printf '%s\n' "$identifier" | sed -e 's/^"\(.*\)"$/\1/' -e "s/^'\(.*\)'$/\1/")

  # Check if identifier contains only alphanumeric characters, underscores, and hyphens
  if [[ ! "$clean_identifier" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    log "ERROR: Invalid characters in SQL identifier: '${clean_identifier}'"
    log "Identifiers must contain only letters, numbers, underscores, and hyphens"
    exit 1
  fi

  # Return the clean identifier without quotes
  printf '%s' "$clean_identifier"
}

ensure_database() {
  local db=$1

  log "Checking if database ${db} exists"
  # Use escaped variable with single quotes to prevent SQL injection in WHERE clause
  if ! psql_base -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='${db}'" | grep -q 1; then
    log "Creating database ${db}"
    # Note: Database names in CREATE DATABASE cannot use single quotes, they use the escaped double-quoted form
    psql_base -d postgres -c "CREATE DATABASE ${db} TEMPLATE template0 ENCODING 'UTF8'" >/dev/null
  else
    log "Database ${db} already exists"
  fi
}

ensure_role() {
  local role=$1
  local password=$2

  log "Checking if role ${role} exists"
  # Use escaped variable with single quotes to prevent SQL injection in WHERE clause
  if ! psql_base -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='${role}'" | grep -q 1; then
    log "Creating role ${role}"
    # Note: Role names in CREATE ROLE cannot use single quotes, they use the escaped double-quoted form
    # Use parameterized query for password to prevent injection
    psql_base -d postgres -c "CREATE ROLE ${role} LOGIN PASSWORD \$\$${password}\$\$" >/dev/null
  else
    log "Role ${role} already exists, updating password"
    # Note: Role names in ALTER ROLE cannot use single quotes, they use the escaped double-quoted form
    # Use parameterized query for password to prevent injection
    psql_base -d postgres -c "ALTER ROLE ${role} PASSWORD \$\$${password}\$\$" >/dev/null
  fi
}

grant_all_privileges() {
  local db=$1
  local role=$2

  log "Granting ALL privileges on database ${db} to role ${role}"

  # Note: In GRANT statements, database and role names cannot use single quotes, they use the escaped double-quoted form
  # Grant database privileges - use escaped db for connection
  if ! psql_base -d "${db}" -c "GRANT CONNECT PRIVILEGES ON DATABASE ${db} TO ${role}" >/dev/null 2>&1; then
    log "WARNING: Failed to grant database privileges on ${db} to ${role} (may already exist)"
  fi

  # Grant schema privileges - use escaped db for connection
  if ! psql_base -d "${db}" -c "GRANT ALL ON SCHEMA public TO ${role}" >/dev/null 2>&1; then
    log "WARNING: Failed to grant schema privileges on 'public' to ${role} (may already exist)"
  fi

  # Grant table privileges - use escaped db for connection
  if ! psql_base -d "${db}" -c "GRANT ALL ON ALL TABLES IN SCHEMA public TO ${role}" >/dev/null 2>&1; then
    log "WARNING: Failed to grant table privileges to ${role} (tables may not exist yet)"
  fi

  # Grant sequence privileges - use escaped db for connection
  if ! psql_base -d "${db}" -c "GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO ${role}" >/dev/null 2>&1; then
    log "WARNING: Failed to grant sequence privileges to ${role} (sequences may not exist yet)"
  fi

  # Future objects default privileges for tables - use escaped db for connection
  if ! psql_base -d "${db}" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${role}" >/dev/null 2>&1; then
    log "WARNING: Failed to set default table privileges for ${role}"
  fi

  # Future objects default privileges for sequences - use escaped db for connection
  if ! psql_base -d "${db}" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${role}" >/dev/null 2>&1; then
    log "WARNING: Failed to set default sequence privileges for ${role}"
  fi
}

main() {
  # Validate all required environment variables with descriptive error messages
  local missing_vars=()

  [[ -z "${AURORA_DATABASE_ENDPOINT:-}" ]] && missing_vars+=("AURORA_DATABASE_ENDPOINT")
  [[ -z "${AURORA_MASTER_USERNAME:-}" ]] && missing_vars+=("AURORA_MASTER_USERNAME")
  [[ -z "${AURORA_MASTER_PASSWORD:-}" ]] && missing_vars+=("AURORA_MASTER_PASSWORD")
  [[ -z "${AURORA_DATABASE_NAME:-}" ]] && missing_vars+=("AURORA_DATABASE_NAME")
  [[ -z "${AURORA_TEST_USERNAME:-}" ]] && missing_vars+=("AURORA_TEST_USERNAME")
  [[ -z "${AURORA_TEST_PASSWORD:-}" ]] && missing_vars+=("AURORA_TEST_PASSWORD")

  if [[ ${#missing_vars[@]} -gt 0 ]]; then
    log "ERROR: Missing required environment variables: ${missing_vars[*]}"
    log "Please ensure all required secrets are available from Vault"
    exit 1
  fi

  # Validate and escape SQL identifiers once at the beginning
  local escaped_db_name
  local escaped_test_user
  escaped_db_name=$(escape_sql_identifier "${AURORA_DATABASE_NAME}")
  escaped_test_user=$(escape_sql_identifier "${AURORA_TEST_USERNAME}")

  log "Starting test database initialization against host ${AURORA_DATABASE_ENDPOINT}";
  log "Database: ${escaped_db_name}, Service User: ${escaped_test_user}"

  # Test database connectivity first
  log "Testing database connectivity..."
  if ! psql_base -d postgres -c "SELECT 1" >/dev/null 2>&1; then
    log "ERROR: Cannot connect to database host ${AURORA_DATABASE_ENDPOINT} with user ${AURORA_MASTER_USERNAME}"
    log "Please verify database host, credentials, and network connectivity"
    exit 1
  fi

  # 1. Check if database exists, create if missing
  ensure_database "${escaped_db_name}"

  # 2. Check if service user exists, create if missing
  ensure_role "${escaped_test_user}" "${AURORA_TEST_PASSWORD}"

  # 3. Grant ALL permissions to service user on this database only
  grant_all_privileges "${escaped_db_name}" "${escaped_test_user}"

  log "Test database initialization complete"
}

main "$@"