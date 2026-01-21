# APP09831-ertm-transaction-consumer-service
Consumer service to consume ERTM Retail transactions and store it in internal DB

*Last Reviewed Date: 25.11.06*

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

Application that is responsible for consuming the retail transaction data from ERTM available in CSV files on AWS s3 bucket and after processing stores the transactions into Aurora DB.

[Design Documentation](https://confluence.nordstrom.com/display/NFT/Data+Integration+Services+%28+Pre+Processor%29+-+High+level+design)

### Application Info

**Application Name:** ERTM Transaction Consumer Service<br>
**AppID:** APP09831<br>
**Tier:** 3<br>
**Email:** TECH_FIN_DATA_INTEGRATION_SUPPORT@nordstrom.com<br>
**Security Group:** TECH_FIN_DATA_INTEGRATION_SECURITY

### Versioning

Application follows the Semantic Versioning (SemVer) scheme, which consists of three numbers:
**major.minor.patch**

**Major:** significant updates, potentially breaking compatibility<br>
**Minor:** new features or enhancements, backward-compatible<br>
**Patch:** bug fixes or minor improvements

<div style="text-align: right" align="right"><a href="#APP09831-ertm-transaction-consumer-service">Back to Top</a></div>

---

## Maintenance:

### Metrics (New Relic):

**Production:** <br>
[Metric Dashboards](TBD)<br>
[Alert Triggers](TBD)

**Non-Production:** <br>
[Metric Dashboards](https://one.newrelic.com/dashboards/detail/Njc3Nzc5OHxWSVp8REFTSEJPQVJEfGRhOjExNTg1ODI2?begin=1762216425137&end=1762302825137&filters=%28name%20LIKE%20%27app09831%27%20OR%20id%20%3D%20%27app09831%27%20OR%20domainId%20%3D%20%27app09831%27%29&state=37c6645a-8017-da14-6bf1-d8c1c0478ec3)<br>
[Alert Triggers](TBD)

### Logs (Splunk):

[Production Logs](https://nordstrom.splunkcloud.com/en-US/app/APP09831/ertm_transaction_consumer_service_prod?form.field1.earliest=-15m&form.field1.latest=now)<br>
[Non-Production Logs](https://nordstrom.splunkcloud.com/en-US/app/APP09831/ertm_transaction_consumer_service_nonprod)<br>

<div style="text-align: right" align="right"><a href="#APP09831-ertm-transaction-consumer-service">Back to Top</a></div>

---

## AWS Details:

### **Production:**

**Name:** prod-fintxdataintx<br>
**Account Number**: 969378265367

### **Non-Production:**

**Name:** nonprod-fintxdataintx<br>
**Account Number**: 007979315855

<div style="text-align: right" align="right"><a href="#APP09831-ertm-transaction-consumer-service">Back to Top</a></div>

---

## Installation:

### Framework and Language

ERTM transaction consumer service is written in Java 21.

### Keys, Secrets, and Certificates Summary

All Keys and Secrets are stored
in [VAULT](https://nonprod-vault.vault.vip.nordstrom.com:8200/ui/vault)

<details>
<summary style="cursor: pointer"><strong>List of All Keys and Secrets</strong></summary>

[//]: # (Table is gropped by ENV (dev/nonprod/prod/any env and then additional vars that is used for tests or any other specific tasks)

| Variable                        | Environment |                 Description                  | Note |
|:--------------------------------|:-----------:|:--------------------------------------------:|:----:|
| NONPROD_AURORA_DATABASE_NAME    | DEV/NONPROD |              Aurora DB Endpoint              |      |
| NONPROD_AURORA_SERVICE_USERNAME | DEV/NONPROD |              Aurora DB Username              |      |
| NONPROD_AURORA_SERVICE_PASSWORD | DEV/NONPROD |              Aurora DB Password              |      |
| NEW_RELIC_LICENSE_KEY           | DEV/NONPROD | The licence key for New Relic authentication |      |
|                                 |             |                                              |      |
| PROD_AURORA_DATABASE_NAME       |    PROD     |              Aurora DB Endpoint              |      |
| PROD_AURORA_SERVICE_USERNAME    |    PROD     |              Aurora DB Username              |      |
| PROD_AURORA_SERVICE_PASSWORD    |    PROD     |              Aurora DB Password              |      |
| NEW_RELIC_LICENSE_KEY           |    PROD     | The licence key for New Relic authentication |      |

</details>

### Setup:

**Prerequisites:** <br>
List of pre-requisites that will be important for building, deploying, testing, contributing, and ultimately
maintaining this repo.

1. Download and install [OpenJDK 21](https://openjdk.java.net/projects/jdk/21/)
2. Download and install [IntelliJ IDEA](https://www.jetbrains.com/idea/download/#section=windows). The community edition
   is good enough.

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

<div style="text-align: right" align="right"><a href="#APP09831-ertm-transaction-consumer-service">Back to Top</a></div>

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

* Aurora DB Postgresql Test Database - existing DB, created specifically for testing purposes.

**List of required environmental variables for Integration Tests:**

* AURORA_TEST_DATABASE_NAME
* AURORA_TEST_POSTGRESQL_DATABASE_ENDPOINT
* AURORA_TEST_POSTGRESQL_PASSWORD
* AURORA_TEST_POSTGRESQL_USERNAME

**Run Instruction:**

Option #1: IDE UI

1. Navigate to *IntegrationTest* directory
2. Modify run configuration of integration test to pass
   the required environmental variables
3. Run integration test

Option #2: Gradle Task

1. Open Terminal
2. Execute command with the required environmental variables:

```
./gradlew integrationTest -Dvariable_name1=variable_value1 -Dvariable_name2=variable_value2
```

</details>


<details>
<summary style="cursor: pointer"><strong>Load Tests</strong></summary>

Load Tests are used to understand how the software performs under various levels of stress and load,
allowing to identify performance bottlenecks and optimize the system for better scalability and reliability.

**Integration Tests are using the following dependencies:**

* Aurora DB Postgresql Test Database - existing DB, created specifically for testing purposes.

</details>

<div style="text-align: right" align="right"><a href="#APP09831-ertm-transaction-consumer-service">Back to Top</a></div>

---

## Running Locally

#### Prerequisites: [Installation](#installation)

1. Run application with IDE UI with modified run configuration: ```Active profiles: local```<br>
   or<br>
   Run application with terminal:
    ```bash
    ./gradlew bootRun --args='--spring.profiles.active=local'
    ```

<div style="text-align: right" align="right"><a href="#APP09831-ertm-transaction-consumer-service">Back to Top</a></div>

---

## Deployment

[Nordstrom Github Actions CI/CD Pipeline](https://developers.nordstromaws.app/docs/TM00434/app09605-github/app09605-customer-docs/github/github-actions-cicd/)

<div style="text-align: right" align="right"><a href="#APP09831-ertm-transaction-consumer-service">Back to Top</a></div>

---
