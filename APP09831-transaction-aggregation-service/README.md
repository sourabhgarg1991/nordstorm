# APP09831-transaction-aggregation-service
Service to aggregate transaction data from Aurora DB and generate CSV files for downstream systems

*Last Reviewed Date: 11.06.25*

<details>
<summary style="cursor: pointer"><strong>Table of Contents</strong></summary>

* [Overview](#overview)
* [Maintenance](#maintenance)
* [AWS Details](#aws-details)
* [Installation](#installation)
* [Testing](#testing)
* [Running Locally](#running-locally)
* [Deployment](#deployment)

</details>

<br>

---

## Overview

Application that aggregates transaction data from Aurora DB based on configurable aggregation rules, generates CSV files (both data and control files), and uploads them to AWS S3 for downstream consumption. The service processes multiple aggregation configurations, maintains transaction-to-aggregation relationships, and provides comprehensive error handling and metrics tracking.

**Key Features:**
- Retrieves aggregation configurations from Aurora DB based on current date
- Executes dynamic aggregation queries to consolidate transaction data
- Generates CSV data files with aggregated transaction information
- Generates control CSV files with metadata (record counts, total amounts)
- Uploads generated files to AWS S3 bucket
- Maintains transaction-aggregation relationship mapping in database
- Retry mechanism for failed S3 uploads
- Comprehensive metrics and error tracking via New Relic
- Idempotent processing with upload status tracking

**Processing Flow:**
1. Retry any previously failed S3 uploads on startup
2. Retrieve current aggregation configurations from Aurora DB
3. For each configuration:
   - Execute aggregation query to consolidate transaction data
   - Generate unique filename with timestamp
   - Execute control data query (if defined) for validation
   - Process and generate control CSV file with metadata
   - Process and generate aggregation data CSV file
   - Save transaction-aggregation relationship mappings
   - Persist generated file details to database
   - Upload files to S3 bucket
   - Update upload status indicators
4. Track metrics and execution times throughout the process

[Design Documentation](https://confluence.nordstrom.com/display/NFT/Data+Integration+Services+%28+Pre+Processor%29+-+High+level+design)

### Application Info

**Application Name:** Transaction Aggregation Service<br>
**AppID:** APP09831<br>
**Tier:** 2<br>
**Email:** TECH_FIN_DATA_INTEGRATION_SUPPORT@nordstrom.com<br>
**Security Group:** TECH_FIN_DATA_INTEGRATION_SECURITY

### Versioning

Application follows the Semantic Versioning (SemVer) scheme, which consists of three numbers:
**major.minor.patch**

**Major:** significant updates, potentially breaking compatibility<br>
**Minor:** new features or enhancements, backward-compatible<br>
**Patch:** bug fixes or minor improvements

<div style="text-align: right" align="right"><a href="#APP09831-transaction-aggregation-service">Back to Top</a></div>

---

## Maintenance

### Metrics (New Relic)

**Production:** <br>
[Metric Dashboards](https://one.newrelic.com/dashboards/detail/Njc3Nzc5N3xWSVp8REFTSEJPQVJEfGRhOjExNjE5NzU4?account=6777797&state=b6e346cb-30d0-7394-d608-86be6cd256cb)<br>
[Alert Triggers](TBD)

**Non-Production:** <br>
[Metric Dashboards](https://one.newrelic.com/dashboards/detail/Njc3Nzc5OHxWSVp8REFTSEJPQVJEfGRhOjExNTkwOTM3?filters=%28name%20LIKE%20%27app09831%27%20OR%20id%20%3D%20%27app09831%27%20OR%20domainId%20%3D%20%27app09831%27%29&state=c1c6e76d-94fb-b856-83b5-c05d4de6844f)<br>
[Alert Triggers](TBD)

### Logs (Splunk)

[Production Logs](https://nordstrom.splunkcloud.com/en-US/app/APP09831/transaction_aggregation_service_prod?form.field1.earliest=-15m&form.field1.latest=now)<br>
[Non-Production Logs](https://nordstrom.splunkcloud.com/en-US/app/APP09831/transaction_aggregation_service_nonprod?form.field1.earliest=-24h&form.field1.latest=now)

<br>

<div style="text-align: right" align="right"><a href="#APP09831-transaction-aggregation-service">Back to Top</a></div>

---

## AWS Details

### **Production:**

**Name:** prod-fintxdataintx<br>
**Account Number**: 969378265367

### **Non-Production:**

**Name:** nonprod-fintxdataintx<br>
**Account Number**: 007979315855

<div style="text-align: right" align="right"><a href="#APP09831-transaction-aggregation-service">Back to Top</a></div>

---

## Installation

### Framework and Language

Transaction aggregation service is written in Java 21 and uses Spring Boot framework.

### Keys, Secrets, and Certificates Summary

All Keys and Secrets are stored
in [VAULT](https://nonprod-vault.vault.vip.nordstrom.com:8200/ui/vault)

<details>
<summary style="cursor: pointer"><strong>List of All Keys and Secrets</strong></summary>

| Variable                                | Environment |                          Description                          | Note |
|:----------------------------------------|:-----------:|:-------------------------------------------------------------:|:----:|
| NONPROD_AURORA_DATABASE_NAME            | DEV/NONPROD |                      Aurora DB Database                       |      |
| NONPROD_AURORA_POSTGRESQL_ENDPOINT      | DEV/NONPROD |                      Aurora DB Endpoint                       |      |
| NONPROD_AURORA_SERVICE_USERNAME         | DEV/NONPROD |                      Aurora DB Username                       |      |
| NONPROD_AURORA_SERVICE_PASSWORD         | DEV/NONPROD |                      Aurora DB Password                       |      |
| NONPROD_AWS_S3_BUCKET_UPLOAD            | DEV/NONPROD |           S3 bucket name for uploading generated files        |      |
| NONPROD_AWS_REGION                      | DEV/NONPROD |                     AWS region for S3 access                  |      |
| NEW_RELIC_LICENSE_KEY                   | DEV/NONPROD |          The licence key for New Relic authentication         |      |
|                                         |             |                                                               |      |
| PROD_AURORA_DATABASE_NAME               |    PROD     |                      Aurora DB Database                       |      |
| PROD_AURORA_POSTGRESQL_ENDPOINT         |    PROD     |                      Aurora DB Endpoint                       |      |
| PROD_AURORA_SERVICE_USERNAME            |    PROD     |                      Aurora DB Username                       |      |
| PROD_AURORA_SERVICE_PASSWORD            |    PROD     |                      Aurora DB Password                       |      |
| PROD_AWS_S3_BUCKET_UPLOAD               |    PROD     |           S3 bucket name for uploading generated files        |      |
| PROD_AWS_REGION                         |    PROD     |                     AWS region for S3 access                  |      |
| NEW_RELIC_LICENSE_KEY                   |    PROD     |          The licence key for New Relic authentication         |      |

</details>

### Setup

**Prerequisites:** <br>
List of pre-requisites that will be important for building, deploying, testing, contributing, and ultimately
maintaining this repo.

1. Download and install [OpenJDK 21](https://openjdk.java.net/projects/jdk/21/)
2. Download and install [IntelliJ IDEA](https://www.jetbrains.com/idea/download/#section=windows). The community edition
   is good enough.
3. AWS credentials configured for S3 access
4. Access to Aurora DB (PostgreSQL) for development/testing

**Installation:** <br>
Once all the prerequisite steps have been completed, the following steps can be used to install the project.

1. Clone the repo
2. Open the project in IDE
3. Build the project using Gradle:
    ```bash 
    ./gradlew clean build
    ```

**Manually format code:** <br>
This project uses [google-java-format](https://plugins.jetbrains.com/plugin/8527-google-java-format) plugin from
Intellij.<br>
The code can be also formatted by executing the following command:

```bash 
./gradlew spotlessApply 
```

**Check dependency tree:**

```bash 
./gradlew dependencies
 ```

<div style="text-align: right" align="right"><a href="#APP09831-transaction-aggregation-service">Back to Top</a></div>

---

## Testing

#### Prerequisites: [Installation](#installation)

<details>
<summary style="cursor: pointer"><strong>Unit Tests</strong></summary>

Unit tests are used to validate that each unit of the software performs as designed,
helping to catch bugs early in the development process and maintain code quality.

**Run Instruction:**

There are two ways to run unit tests.

From terminal or cli:

```bash 
./gradlew test 
```

From IDE:
1. Navigate to the test class
2. Right-click and select "Run Tests"

**Test Coverage:**

To generate test coverage report:

```bash 
./gradlew jacocoTestReport
```

View the report at: `build/reports/jacoco/test/html/index.html`

</details>

<details>
<summary style="cursor: pointer"><strong>Integration Tests</strong></summary>

Integration Tests are used to verify that different components of the software interact correctly
with each other, ensuring that the system functions as intended when all parts are combined.

**Integration Tests are using the following dependencies:**

* Aurora DB Postgresql Test Database - existing DB, created specifically for testing purposes
* AWS S3 (or mock S3 service for local testing)

**List of required environmental variables for Integration Tests:**

* AURORA_TEST_DATABASE_NAME
* AURORA_TEST_POSTGRESQL_DATABASE_ENDPOINT
* AURORA_TEST_POSTGRESQL_PASSWORD
* AURORA_TEST_POSTGRESQL_USERNAME
* AWS_S3_BUCKET_UPLOAD_TEST (optional, for S3 integration testing)

> **Note:** Please reach out to code owners for credentials to access the test database.

**Run Instruction:**

Option #1: IDE UI

1. Navigate to *integrationTest* directory
2. Modify run configuration of integration test to pass the required environmental variables
3. Run integration test

Option #2: Gradle Task

1. Open Terminal
2. Execute command with the required environmental variables:

```bash
./gradlew integrationTest -DAURORA_TEST_DATABASE_NAME=value1 -DAURORA_TEST_POSTGRESQL_DATABASE_ENDPOINT=value2 -DAURORA_TEST_POSTGRESQL_PASSWORD=value3 -DAURORA_TEST_POSTGRESQL_USERNAME=value4
```

</details>

<div style="text-align: right" align="right"><a href="#APP09831-transaction-aggregation-service">Back to Top</a></div>

---

## Running Locally

#### Prerequisites: [Installation](#installation)

**⚠️ Important Notice:** Running this service locally requires proper configuration of AWS credentials and Aurora DB access.

### Option 1: Run as Spring Boot Application

**Required Environment Variables:**

Set the following environment variables before running:

```bash
export AURORA_DATABASE_NAME=your_db_name
export AURORA_POSTGRESQL_ENDPOINT=your_db_endpoint
export AURORA_SERVICE_USERNAME=your_username
export AURORA_SERVICE_PASSWORD=your_password
export AWS_S3_BUCKET_UPLOAD=your_s3_bucket
export AWS_REGION=us-west-2
export NEW_RELIC_LICENSE_KEY=your_newrelic_key
```

**Run from IDE:**

1. Open `Application.java` in IntelliJ IDEA
2. Configure run configuration with environment variables
3. Run the main method

**Run from Terminal:**

```bash
./gradlew bootRun
```

Or using the JAR:

```bash
./gradlew bootJar
java -jar build/libs/transaction-aggregation-service-1.0.0.jar
```

### Option 2: Run with Docker

Build the Docker image:

```bash
docker build -t transaction-aggregation-service:latest .
```

Run the container:

```bash
docker run -e AURORA_DATABASE_NAME=your_db_name \
  -e AURORA_POSTGRESQL_ENDPOINT=your_db_endpoint \
  -e AURORA_SERVICE_USERNAME=your_username \
  -e AURORA_SERVICE_PASSWORD=your_password \
  -e AWS_S3_BUCKET_UPLOAD=your_s3_bucket \
  -e AWS_REGION=us-west-2 \
  transaction-aggregation-service:latest
```

### Option 3: Trigger Feature Branch CronJob Manually (Recommended)

Instead of running locally, it's recommended to trigger a feature branch CronJob manually in the Kubernetes cluster for testing and debugging purposes.

**Steps to manually trigger a feature branch CronJob:**

1. **List available CronJobs:**
   ```bash
   kubectl get cronjob -n app09831 --as app09831-sudo
   ```

2. **Create a manual job from the CronJob:**
   ```bash
   kubectl -n app09831 create job manual-debug-$(date +%s) --from=cronjob/transaction-aggregation-service-{feature_branch} --as app09831-sudo
   ```
   Replace `{feature_branch}` with your actual feature branch name.

3. **Get the pod name and follow logs:**
   ```bash
   POD_NAME=$(kubectl get pods -n app09831 --as app09831-sudo --sort-by=.metadata.creationTimestamp | grep manual-debug | tail -1 | awk '{print $1}')
   kubectl logs -n app09831 $POD_NAME --as app09831-sudo -f --timestamps
   ```

This approach allows you to test your changes in an environment that has proper AWS credentials and database access configured.

<div style="text-align: right" align="right"><a href="#APP09831-transaction-aggregation-service">Back to Top</a></div>

---

## Deployment

This service uses [GitHub Actions CI/CD Pipeline](https://github.com/Nordstrom-Internal/APP09831-transaction-aggregation-service/actions) for automated deployment.

### Deployment Workflow

The service is deployed as a Kubernetes CronJob that runs on a scheduled basis to aggregate transaction data and generate files.

**GitHub Actions Workflow:**
- Automated builds on pull requests and merges to main branch
- Automated testing (unit and integration tests)
- Code quality checks (spotless, linting)
- Docker image building and pushing to container registry
- Kubernetes deployment updates
- Environment-specific deployments (dev, nonprod, prod)

**Access CI/CD Pipeline:**
[APP09831 Transaction Aggregation Service - GitHub Actions](https://github.com/Nordstrom-Internal/APP09831-transaction-aggregation-service/actions)

### CronJob Schedule

The service runs as a scheduled Kubernetes CronJob. The schedule can be configured in the Kubernetes manifests located in:
- Non-production: `k8s/nonprod/`
- Production: `k8s/prod/`

### Manual Deployment

To manually deploy to a specific environment, use the GitHub Actions workflow dispatch or deploy using kubectl:

```bash
kubectl apply -f k8s/nonprod/ -n app09831 --as app09831-sudo
```
