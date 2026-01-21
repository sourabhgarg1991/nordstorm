# APP09831-promotion-transaction-consumer-service
Consumer service to fetch promotion transaction data from GCP BigQuery and persist it in Aurora DB

*Last Reviewed Date: 11.05.25*

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

Application that fetches promotion transaction data from GCP BigQuery (promotion golden dataset), processes the data, and stores transactions in Aurora DB. The service maps GCP BigQuery rows to three-tier entity structure: Transaction → TransactionLine → PromotionTransactionLine.

**Key Features:**
- Fetches promotion transaction data from GCP BigQuery in paginated batches
- Performs duplicate checking based on global transaction IDs
- Maps BigQuery records to JPA entities (Transaction, TransactionLine, PromotionTransactionLine)
- Asynchronous batch processing with configurable thread pools
- Comprehensive metrics and error tracking
- Idempotent processing to prevent duplicate records

**Processing Flow:**
1. Query GCP BigQuery for promotion transaction data (LOYALTY_PROMO and MARKETING_PROMO)
2. Fetch data in configurable page sizes (default: as configured in `GCP_PROMOTION_DATA_PAGE_SIZE`)
3. Process each page asynchronously
4. Check for existing transactions in Aurora DB by global transaction ID
5. Filter out duplicates
6. Map new transactions to entity graph (Transaction → TransactionLine → PromotionTransactionLine)
7. Persist to Aurora DB using JPA cascade operations

[Design Documentation](https://confluence.nordstrom.com/display/NFT/Data+Integration+Services+%28+Pre+Processor%29+-+High+level+design)

### Application Info

**Application Name:** Promotion Transaction Consumer Service<br>
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

<div style="text-align: right" align="right"><a href="#APP09831-promotion-transaction-consumer-service">Back to Top</a></div>

---

## Maintenance

### Metrics (New Relic)

**Production:** <br>
[Metric Dashboards](TBD)<br>
[Alert Triggers](TBD)

**Non-Production:** <br>
[Metric Dashboards](https://onenr.io/0ZQWBbvY3jW)<br>
[Alert Triggers](TBD)

### Logs (Splunk)

[Production Logs](https://nordstrom.splunkcloud.com/en-US/app/APP09831/promotion_transaction_consumer_service_prod)<br>
[Non-Production Logs](https://nordstrom.splunkcloud.com/en-US/app/APP09831/promotion_transaction_consumer_service_nonprod)

<br>

<div style="text-align: right" align="right"><a href="#APP09831-promotion-transaction-consumer-service">Back to Top</a></div>

---

## AWS Details

### **Production:**

**Name:** prod-fintxdataintx<br>
**Account Number**: 969378265367

### **Non-Production:**

**Name:** nonprod-fintxdataintx<br>
**Account Number**: 007979315855

<div style="text-align: right" align="right"><a href="#APP09831-promotion-transaction-consumer-service">Back to Top</a></div>

---

## Installation

### Framework and Language

Promotion transaction consumer service is written in Java 21.

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
| NONPROD_PROMO_SOURCE_GCP_PROJECT_ID     | DEV/NONPROD |          GCP NAP Project ID for source data access            |      |
| NONPROD_GCP_PROJECT_ID                  | DEV/NONPROD |              GCP Project ID for service resources             |      |
| NONPROD_PROMO_SOURCE_GCP_DATASET        | DEV/NONPROD |               BigQuery dataset containing promo data          |      |
| NONPROD_PROMO_SOURCE_GCP_TABLE_NAME     | DEV/NONPROD |                BigQuery table name for promo data             |      |
| NONPROD_GCP_PROMOTION_DATA_PAGE_SIZE    | DEV/NONPROD |      Number of records to fetch per page from BigQuery        |      |
| START_DATE_OVERRIDE                     | DEV/NONPROD |     Optional: Override start date for data fetch (ISO 8601)   |      |
| END_DATE_OVERRIDE                       | DEV/NONPROD |      Optional: Override end date for data fetch (ISO 8601)    |      |
| NEW_RELIC_LICENSE_KEY                   | DEV/NONPROD |          The licence key for New Relic authentication         |      |
|                                         |             |                                                               |      |
| PROD_AURORA_DATABASE_NAME               |    PROD     |                      Aurora DB Database                       |      |
| PROD_AURORA_POSTGRESQL_ENDPOINT         |    PROD     |                      Aurora DB Endpoint                       |      |
| PROD_AURORA_SERVICE_USERNAME            |    PROD     |                      Aurora DB Username                       |      |
| PROD_AURORA_SERVICE_PASSWORD            |    PROD     |                      Aurora DB Password                       |      |
| PROD_PROMO_SOURCE_GCP_PROJECT_ID        |    PROD     |          GCP NAP Project ID for source data access            |      |
| PROD_GCP_PROJECT_ID                     |    PROD     |              GCP Project ID for service resources             |      |
| PROD_PROMO_SOURCE_GCP_DATASET           |    PROD     |               BigQuery dataset containing promo data          |      |
| PROD_PROMO_SOURCE_GCP_TABLE_NAME        |    PROD     |                BigQuery table name for promo data             |      |
| PROD_GCP_PROMOTION_DATA_PAGE_SIZE       |    PROD     |      Number of records to fetch per page from BigQuery        |      |
| START_DATE_OVERRIDE                     |    PROD     |     Optional: Override start date for data fetch (ISO 8601)   |      |
| END_DATE_OVERRIDE                       |    PROD     |      Optional: Override end date for data fetch (ISO 8601)    |      |
| NEW_RELIC_LICENSE_KEY                   |    PROD     |          The licence key for New Relic authentication         |      |

</details>

### Setup

**Prerequisites:** <br>
List of pre-requisites that will be important for building, deploying, testing, contributing, and ultimately
maintaining this repo.

1. Download and install [OpenJDK 21](https://openjdk.java.net/projects/jdk/21/)
2. Download and install [IntelliJ IDEA](https://www.jetbrains.com/idea/download/#section=windows). The community edition
   is good enough.
3. GCP credentials file (`gcp-credentials-nonprod.json`) - mounted at `/opt/gcp-credentials-nonprod.json` in container

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

<div style="text-align: right" align="right"><a href="#APP09831-promotion-transaction-consumer-service">Back to Top</a></div>

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

</details>

<details>
<summary style="cursor: pointer"><strong>Integration Tests</strong></summary>

Integration Tests are used to verify that different components of the software interact correctly
with each other, ensuring that the system functions as intended when all parts are combined.

**Integration Tests are using the following dependencies:**

* Aurora DB Postgresql Test Database - existing DB, created specifically for testing purposes and shared with ERTM consumer service.

**List of required environmental variables for Integration Tests:**

* AURORA_TEST_DATABASE_NAME
* AURORA_TEST_POSTGRESQL_DATABASE_ENDPOINT
* AURORA_TEST_POSTGRESQL_PASSWORD
* AURORA_TEST_POSTGRESQL_USERNAME

> **Note:** Please reach out to code owners for credentials to access the test database.

**Run Instruction:**

Option #1: IDE UI

1. Navigate to *IntegrationTest* directory
2. Modify run configuration of integration test to pass the required environmental variables
3. Run integration test

Option #2: Gradle Task

1. Open Terminal
2. Execute command with the required environmental variables:

```bash
./gradlew integrationTest -Dvariable_name1=variable_value1 -Dvariable_name2=variable_value2
```

</details>

<div style="text-align: right" align="right"><a href="#APP09831-promotion-transaction-consumer-service">Back to Top</a></div>

---

## Running Locally

#### Prerequisites: [Installation](#installation)

**⚠️ Important Notice:** Running this service locally is **not recommended** due to the requirement for GCP credentials and BigQuery access.

### Recommended Approach: Trigger Feature Branch CronJob Manually

Instead of running locally, it's recommended to trigger a feature branch CronJob manually in the Kubernetes cluster for testing and debugging purposes.

**Steps to manually trigger a feature branch CronJob:**

1. **List available CronJobs:**
   ```bash
   kubectl get cronjob -n app09831 --as app09831-sudo
   ```

2. **Create a manual job from the CronJob:**
   ```bash
   kubectl -n app09831 create job manual-debug-$(date +%s) --from=cronjob/promotion-transaction-consumer-service-{feature_branch} --as app09831-sudo
   ```
   Replace `{feature_branch}` with your actual feature branch name.

3. **Get the pod name and follow logs:**
   ```bash
   POD_NAME=$(kubectl get pods -n app09831 --as app09831-sudo --sort-by=.metadata.creationTimestamp | grep manual-debug | tail -1 | awk '{print $1}')
   kubectl logs -n app09831 $POD_NAME --as app09831-sudo -f --timestamps
   ```

This approach allows you to test your changes in an environment that has proper GCP credentials and network access configured.

<div style="text-align: right" align="right"><a href="#APP09831-promotion-transaction-consumer-service">Back to Top</a></div>

---

## Deployment

This service uses [GitHub Actions CI/CD Pipeline](https://github.com/Nordstrom-Internal/APP09831-promotion-transaction-consumer-service/actions) for automated deployment.

### Deployment Workflow

The service is deployed as a Kubernetes CronJob that runs on a scheduled basis to fetch and process promotion transaction data.

**GitHub Actions Workflow:**
- Automated builds on pull requests and merges to main branch
- Automated testing (unit and integration tests)
- Docker image building and pushing to container registry
- Kubernetes deployment updates
- Environment-specific deployments (dev, nonprod, prod)

**Access CI/CD Pipeline:**
[APP09831 Promotion Transaction Consumer Service - GitHub Actions](https://github.com/Nordstrom-Internal/APP09831-promotion-transaction-consumer-service/actions)
