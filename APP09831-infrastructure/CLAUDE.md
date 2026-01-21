# APP09831 Infrastructure Documentation

## Overview

This repository contains the infrastructure as code (IaC) for APP09831, a Data Integration application at Nordstrom. It manages AWS resources and Confluent Cloud Kafka resources required for financial transaction data processing from retail and restaurant operations.

## Application Metadata

- **App ID**: APP09831
- **App Name**: Infrastructure / Data Integration
- **Primary Region**: us-west-2 (US West - Oregon)
- **Environments**: dev, nonprod, prod

## Architecture Components

### 1. AWS Aurora PostgreSQL Database

**File**: [terraform/aws-aurora-db.tf](terraform/aws-aurora-db.tf)

The infrastructure provisions an Aurora PostgreSQL cluster for storing transaction data:

- **Engine**: aurora-postgresql version 16.6
- **Family**: aurora-postgresql16
- **Port**: 5432
- **Instance Classes**:
  - Production: db.r6g.2xlarge
  - Non-Production: db.t4g.large
- **High Availability**:
  - 1 instance in nonprod
  - 2 instances in prod (multi-AZ)
- **Security Features**:
  - IAM database authentication enabled
  - Storage encryption enabled
  - Deletion protection enabled in production
  - Backup retention: 7 days (prod), 1 day (nonprod)

**Security Groups**: Custom security group allows access from:
- Nordstrom domain (10.0.0.0/8)
- VPN access (161.181.53.0/24)
- NSK cluster internal IPs (managed prefix lists)
- GitHub runners (managed prefix lists)

### 2. S3 Buckets

**File**: [terraform/s3-bucket.tf](terraform/s3-bucket.tf)

Three S3 buckets are created per environment:

1. **ERTM Transaction Source Bucket** (`app09831-ertm-transaction-source-{env}`)
   - Receives transaction data from RedShift Services
   - Lifecycle: Data expires after 7 days (dev), 30 days (nonprod), 90 days (prod)
   - Access: RedShift Services can PUT objects

2. **ERTM Transaction Processed Bucket** (`app09831-ertm-transaction-processed-{env}`)
   - Stores processed transaction data
   - Same lifecycle policy as source bucket

3. **ERTM Query Source Bucket** (`app09831-ertm-query-source-{env}`)
   - Stores SQL query files for data extraction
   - Access: Teradata EC2 instances can READ objects
   - Contains: ertm_aedw_transaction_data_extract.sql

One S3 buckets are created for nonprod environment:

1. **ERTM Transaction Source Bucket** (`app09831-data-integration-test`)
    - Receives transaction data from RedShift Services
    - Lifecycle: Data expires after 1 day (nonprod)
    - Access: RedShift Services can PUT objects

### 3. Confluent Cloud Kafka Topics

**File**: [terraform/confluent-cloud-kafka-topic.tf](terraform/confluent-cloud-kafka-topic.tf)

Kafka topics are provisioned for transaction processing with retry and dead-letter-queue (DLT) patterns:

#### Standard Transaction Topics (Nonprod & Prod)

1. **Retail Transaction Topics**:
   - `customer-financial-retail-transaction-operational-avro-data-integration-dlt`
   - `customer-financial-retail-transaction-operational-avro-data-integration-retry-0`

2. **Restaurant Transaction Topics**:
   - `customer-financial-restaurant-transaction-operational-avro-data-integration-dlt`
   - `customer-financial-restaurant-transaction-operational-avro-data-integration-retry-0`

**Configuration** (`cc_topic_config`):
- Partition count: 3 (prod), 1 (nonprod)
- Retention: 75 days (6,480,000,000 ms)
- Clusters:
  - Nonprod: columbia-nonprod (lkc-09v3v2)
  - Prod: columbia-prod (lkc-3nwdnj)
- Message size: 1 MB max
- Cleanup policy: delete

#### Internal Topics (Nonprod Only)

3. **Restaurant Transaction DI Internal Topics**:
   - `customer-financial-restaurant-transaction-operational-di-internal-avro` (main topic)
   - `customer-financial-restaurant-transaction-operational-di-internal-avro-data-integration-dlt` (DLT)
   - `customer-financial-restaurant-transaction-operational-di-internal-avro-data-integration-retry-0` (retry)

4. **Retail Transaction DI Internal Topics**:
   - `customer-financial-retail-transaction-operational-di-internal-avro` (main topic)
   - `customer-financial-retail-transaction-operational-di-internal-avro-data-integration-dlt` (DLT)
   - `customer-financial-retail-transaction-operational-di-internal-avro-data-integration-retry-0` (retry)

**Configuration** (`cc_internal_topic_config`):
- Environment: Nonprod only
- Partition count: 1
- Retention: 1 hour (3,600,000 ms)
- Cluster: columbia-nonprod (lkc-09v3v2)
- Message size: 1 MB max
- Cleanup policy: delete
- Delete retention: 1 hour (3,600,000 ms)

**Total Topics**: 10 (4 standard topics in nonprod & prod, 6 internal topics in nonprod only)

### 4. IAM Roles

**File**: [terraform/roles.tf](terraform/roles.tf)

**K8s S3 Access Role** (`fin-data-integration-k8s-access-role-{env}`):
- Allows Kubernetes pods in namespace `app09831` to assume role via OIDC
- Permissions:
  - Full S3 access (AmazonS3FullAccess)
  - RDS database connection (rds-db:connect)
  - PutObject to ERP financial S3 buckets

**Deployment Role**: APP09831_Deployment_Role
- Attached policy: AmazonRDSFullAccess
- Used by CI/CD pipelines

### 5. Database Migration

**File**: [build.gradle](build.gradle)

Database schema management is handled by Flyway:
- **Version**: 11.11.0
- **Java Version**: 21
- **PostgreSQL Driver**: 42.7.7
- **Migration Location**: `src/main/resources/database/migration`
- **Schema**: public

**Migration Files** (in chronological order):
1. V1: Create initial tables
2. V2: Insert aggregation configuration data
3. V3: Update transaction table column name
4. V4: Update aggregation configuration data
5. V5: Rename aggregation configuration table columns
6. V6: Update aggregation configuration data to amounts zero
7. V7: Add upload indicator to generated file detail table
8. V8: Rename aggregation file name prefix and update query for CRP2 data
9. V9: Update aggregation control query for CRP2 data
10. V10: Update table sequences increment
11. V11: Update table sequences increment to 2000
12. V12: Add/update is_published_to_data_platform column

## Network Configuration

### VPCs
- **Dev/Nonprod**: vpc-0ed8a34dc686f0ad9
- **Prod**: vpc-0d70617ba8d47b8dc

### Subnets (3 AZs per environment)
- **Dev/Nonprod**:
  - subnet-07628c5c016ba782f (us-west-2a)
  - subnet-0db7f6c08972d8677 (us-west-2b)
  - subnet-0be8e8681614d8436 (us-west-2c)
- **Prod**:
  - subnet-085ef01ac54c12526 (us-west-2a)
  - subnet-0d3929bd18c40d175 (us-west-2b)
  - subnet-0e191f7b304455a7e (us-west-2c)

### Security Groups
- **Dev/Nonprod**: sg-068c5e283f90fde54
- **Prod**: sg-0a96fe00505dbcd2a

## AWS Accounts

- **Dev/Nonprod**: 007979315855
- **Prod**: 969378265367

## CI/CD Workflows

### Nonprod Pipeline

**File**: [.github/workflows/nonprod.yml](.github/workflows/nonprod.yml)

**Trigger**: Push to `release/nonprod` branch or manual dispatch

**Jobs**:
1. terraform-check: Validates Terraform configuration
2. terraform-plan: Plans infrastructure changes
3. terraform-apply: Applies changes (only on push)

**Backend**: S3 in account 007979315855

### Production Pipeline

**File**: [.github/workflows/production.yml](.github/workflows/manual-production.yml)

**Trigger**: Push to `main` branch or manual dispatch

**Jobs**:
1. terraform-check: Validates Terraform configuration
2. create-change-request: Creates ServiceNow change request
3. terraform-plan: Plans infrastructure changes
4. terraform-apply: Applies changes after approval

**Backend**: S3 in account 969378265367

### Feature Branch Pipeline

**File**: [.github/workflows/feature-branch.yml](.github/workflows/feature-branch.yml)

Runs terraform check and plan on feature branches without applying changes.

### Manual Flyway Migration Workflows

**Files**:
- [.github/workflows/manual-flyway-migration-nonprod.yml](.github/workflows/manual-flyway-migration-nonprod.yml)
- [.github/workflows/manual-flyway-migration-prod.yml](.github/workflows/manual-flyway-migration-prod.yml)

Manually triggered database migrations using Flyway.

## Terraform Configuration

### Required Versions
- **Terraform**: >= 1.9, < 2
- **AWS Provider**: >= 5, < 6
- **Confluent Cloud Provider**: >= 0.12, < 1 (from Nordstrom Artifactory)

### Variables

**File**: [terraform/variables.tf](terraform/variables.tf)

Required variables:
- `app_id`: Application NERDS ID
- `app_name`: Application name
- `environment`: Deployment environment
- `env_app_name`: Environment-specific app name
- `aurora_database_name`: Database name
- `aurora_master_username`: Database admin username
- `aurora_master_password`: Database admin password (sensitive)
- `rds_cidr_blocks`: Allowed CIDR blocks for RDS access
- `oauth_client_id`: Confluent Cloud OAuth client ID
- `oauth_client_secret`: Confluent Cloud OAuth secret (sensitive)

### Locals Configuration

**File**: [terraform/locals.tf](terraform/locals.tf)

Key local values:
- Environment detection logic (dev, nonprod, prod)
- Database configuration parameters
- Kafka cluster IDs and topic names (including DI internal topics)
- Cross-account role ARNs for Teradata and RedShift
- VPC, subnet, and security group mappings
- OIDC provider hashes for IRSA

**Kafka Topic Configurations**:

1. **cc_topic_config**: Standard topic configuration
   - Used for: Standard DLT and retry topics (prod & nonprod)
   - Retention: 75 days (6,480,000,000 ms)
   - Max message size: 1,048,588 bytes (~1 MB)
   - Cleanup policy: delete

2. **cc_internal_topic_config**: Internal topic configuration
   - Used for: DI internal topics (nonprod only)
   - Retention: 1 hour (3,600,000 ms)
   - Delete retention: 1 hour (3,600,000 ms)
   - Max message size: 1,048,588 bytes (~1 MB)
   - Cleanup policy: delete
   - Purpose: Short-lived internal processing topics for data integration workflows

## External Integrations

### Teradata
- **Role ARN (Nonprod)**: arn:aws:iam::383131257218:role/TD-UTILITIES-EC2
- **Role ARN (Prod)**: arn:aws:iam::773908631549:role/TD-UTILITIES-EC2
- **Access**: Read from ERTM query source bucket

### RedShift Services
- **User ARN (Nonprod)**: arn:aws:iam::542502476764:user/User-NonProd-RedshiftServices-542502476764
- **User ARN (Prod)**: arn:aws:iam::975757171738:user/User-Prod-RedshiftServices-975757171738
- **Access**: Write to ERTM transaction source bucket

### ERP Services
- **S3 Buckets**:
  - Dev: data-integration-services-input-dev
  - Nonprod: data-integration-services-input-nonprod
  - Prod: data-integration-services-input-prod
- **Access**: K8s pods can PutObject

## Database Initialization

**Files**:
- [db_script/init-main-database.sh](db_script/init-main-database.sh)
- [db_script/init-test-database.sh](db_script/init-test-database.sh)

Scripts for initializing main and test databases.

## Repository Configuration

**File**: [.github/repository.yml](.github/repository.yml)

- **Default Branch**: release/nonprod
- **Merge Strategy**: Squash merge allowed

## Key Design Decisions

1. **Environment Strategy**: Resources are only created in nonprod and prod environments. Feature branches use dev account but resources are not provisioned.

2. **Database High Availability**: Production uses 2 Aurora instances for redundancy, while nonprod uses a single instance to reduce costs.

3. **Data Retention**: Progressive retention policies (7/30/90 days for dev/nonprod/prod) balance storage costs with compliance requirements.

4. **Kafka Retry Pattern**: Separate retry and DLT topics for both retail and restaurant transactions enable sophisticated error handling.

5. **Internal Topics Strategy**: DI internal topics (6 topics) are created only in nonprod for testing and development of internal data integration workflows. These topics use short retention (1 hour) to minimize storage costs since they are used for transient processing only.

6. **Dual Topic Configurations**: Two Kafka topic configurations support different use cases:
   - `cc_topic_config`: 75-day retention for standard operational topics
   - `cc_internal_topic_config`: 1-hour retention for ephemeral internal processing topics

7. **IAM Authentication**: Database uses both password and IAM authentication for flexibility and security.

8. **GitOps**: Infrastructure changes follow GitOps principles with environment-specific branches (release/nonprod, main).

9. **Change Management**: Production deployments require ServiceNow change requests for audit and compliance.

## Maintenance Notes

### Applying Database Changes Manually

By default, database modifications are deferred to the maintenance window (`aurora_postgresql_apply_immediately = false`). To apply immediately:

```bash
aws rds modify-db-instance \
  --db-instance-identifier <instance-name> \
  --region us-west-2 \
  --apply-immediately
```

### Terraform State

- **Backend**: S3
- **State File**: main
- **Region**: us-west-2
- **Locking**: DynamoDB (managed by APP02944 marketplace workflows)

## Tags

All resources are tagged with:
- `AppId`: APP09831
- `AppName`: Infrastructure
- `Project`: Infrastructure
- `Environment`: dev/nonprod/prod
- `Location`: us-west-2

## Related Documentation

- [Nordstrom AWS Clusters](https://developers.nordstromaws.app/docs/TM00458/APP09285-customer-documentation/clusters/aws/)
- [AWS Managed Prefix Lists](https://confluence.nordstrom.com/display/PubCloud/AWS+Managed+Prefix+Lists)
- [APP02944 Internal Marketplace Workflows](https://github.com/Nordstrom-Internal/APP02944-internal-marketplace)

## Support

For questions or issues with this infrastructure:
1. Check GitHub Actions workflow runs for deployment status
2. Review Terraform plan outputs before applying changes
3. Contact the Data Integration team for application-specific questions
