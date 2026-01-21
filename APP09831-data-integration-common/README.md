# Data Integration Commons Library

*Last Reviewed Date: 2025-10-16*

<details>
<summary style="cursor: pointer"><strong>Table of Contents</strong></summary>

* [Overview](#overview)
* [Installation](#installation)
* [Testing](#testing)
* [Technical Documentation](#technical-documentation)
    * [Metrics Client](#metrics-client)
    * [MoneyUtility](#moneyutility)
    * [DateTimeFormatUtility](#datetimeformatutility)
    * [StringFormatUtility](#stringformatutility)

</details>

<br>

---

## Overview

This is a Java 21 library for shared utilities of the Data Integration project.

### Versioning

Library follows the Semantic Versioning (SemVer) scheme, which consists of three numbers:
**major.minor.patch**

**Major:** significant updates, potentially breaking compatibility<br>
**Minor:** new features or enhancements, backward-compatible<br>
**Patch:** bug fixes or minor improvements

**Current Version:** 1.0.1

<div style="text-align: right" align="right"><a href="#data-integration-commons-library">Back to Top</a></div>

---

## Installation

To use this library in your project, add the following dependency to your `build.gradle`:

```gradle
ext {
    dataIntegrationCommon = '1.0.1'
}

dependencies {
    implementation "com.nordstrom.finance.dataintegration.common:data-integration-common:${dataIntegrationCommon}"
}
```

### Artifactory Configuration

This application uses Artifactory for dependency management. To run locally, provide your Artifactory credentials using Gradle properties. Your personal credentials are available in your [Artifactory user profile](https://artifactory.nordstrom.com/ui/admin/artifactory/user_profile).

Add the following properties to your `$GRADLE_USER_HOME/gradle.properties` file (typically `~/.gradle/gradle.properties`):

- `artifactoryUsername`
- `artifactoryPassword`

_When running GitHub Actions builds, Artifactory credentials are automatically included._

## Building the project

```bash
./gradlew build
```

<div style="text-align: right" align="right"><a href="#data-integration-commons-library">Back to Top</a></div>

---

## Testing

### Unit Tests

Unit tests are used to validate that each unit of the software performs as designed, helping to catch bugs early in the development process and maintain code quality.

**Run Instruction:**

```bash 
./gradlew test 
```

<div style="text-align: right" align="right"><a href="#data-integration-commons-library">Back to Top</a></div>

---

## Technical Documentation

### Metrics Client

The MetricsClient provides StatsD-based metrics collection for sending application metrics to New Relic via Datadog StatsD protocol.

#### Configuration

Add the following to your `application.yml`:

```yaml
metrics:
  hostname: gostatsd.kube-system.svc
  port: 8125
  prefix: your-app-name
  constant-tags:
    - env:${ENVIRONMENT:dev}
    - app:your-app-name
    - version:${APP_VERSION:1.0.1}
```

#### Spring Bean Configuration

Create a configuration class:

```java
@Configuration
public class MetricConfig {

  @Bean
  @ConfigurationProperties(prefix = "metrics", ignoreUnknownFields = false)
  public MetricsConfig metricsConfig() {
    return new MetricsConfig();
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
  public MetricsClient metricsClient(final MetricsConfig config) {
    return MetricsClient.createClient(config);
  }
}
```

#### Usage Examples

```java
@Service
@RequiredArgsConstructor
public class YourService {
  
  private final MetricsClient metricsClient;
  
  public void processRecord() {
    // Increment counter
    metricsClient.incrementCounter("record.processed");
    
    // Record execution time
    long startTime = System.currentTimeMillis();
    // ... do work ...
    metricsClient.recordExecutionTime("operation.duration", 
        System.currentTimeMillis() - startTime);
    
    // Increment error count with tags
    metricsClient.incrementErrorCount(
        MetricsCommonTag.ERROR_CODE.getTag("DB_ERROR"),
        MetricsCommonTag.TOPIC_NAME.getTag("orders"));
    
    // Record gauge value
    metricsClient.recordGaugeValue("queue.size", queueSize);
  }
}
```

#### Available Metric Tags

The library provides standard metric tags via `MetricsCommonTag` enum:

- `ERROR_CODE` - Error code tagging
- `ENTRY_TYPE` - Entry type classification
- `MISSING_LOOKUP` - Missing lookup identification
- `TOPIC_NAME` - Kafka topic or message source

<div style="text-align: right" align="right"><a href="#data-integration-commons-library">Back to Top</a></div>

---

### MoneyUtility

The MoneyUtility provides conversion between monetary units/nanos and BigDecimal amounts, following Google Money API conventions.

#### Usage Examples

```java
// Convert units and nanos to BigDecimal
BigDecimal amount = MoneyUtility.getAmount(10L, 500_000_000); // Returns 10.50

// Parse from strings
BigDecimal amount = MoneyUtility.getAmount("15", "250000000"); // Returns 15.25

// Extract units from amount
String units = MoneyUtility.getUnitsFromAmount(new BigDecimal("123.45")); // Returns "123"

// Extract nanos from amount
String nanos = MoneyUtility.getNanosFromAmount(new BigDecimal("10.50")); // Returns "500000000"
```

#### Important Notes

- All monetary amounts are rounded to 2 decimal places
- Use absolute values for monetary amounts as per project standards

<div style="text-align: right" align="right"><a href="#data-integration-commons-library">Back to Top</a></div>

---

### DateTimeFormatUtility

The DateTimeFormatUtility provides standardized date and time formatting for database persistence, timestamps, and timezone conversions.

#### Standard Formats

The utility supports the following standard formats:

| Format | Pattern | Use Case |
|--------|---------|----------|
| Simple Date | `yyyy-MM-dd` | Database persistence (Aurora PostgreSQL) |
| Timestamp (Milliseconds) | `yyyy-MM-dd HH:mm:ss.SSS` | Standard logging |
| Timestamp (Microseconds) | `yyyy-MM-dd HH:mm:ss.SSSSSS` | High-precision timestamps (UTC) |
| Compact Date | `MMddyyyy` | File naming, legacy systems |

#### Usage Examples

**Database Persistence (yyyy-MM-dd):**

```java
// Format for database storage
String dateStr = DateTimeFormatUtility.formatToSimpleDate(LocalDate.now());
// Returns: "2025-10-16"

// Parse from database
LocalDate date = DateTimeFormatUtility.parseSimpleDate("2025-10-16");

// Get current date
String today = DateTimeFormatUtility.getCurrentSimpleDate();
```

**Timestamps:**

```java
// Timestamp with milliseconds
String timestamp = DateTimeFormatUtility.formatToTimestampMilliseconds(LocalDateTime.now());
// Returns: "2025-10-16 14:30:45.123"

// Timestamp with microseconds (UTC)
String timestampMicro = DateTimeFormatUtility.formatToTimestampMicroseconds(LocalDateTime.now());
// Returns: "2025-10-16 14:30:45.123456"

// Current timestamp
String now = DateTimeFormatUtility.getCurrentTimestampMilliseconds();
```

**Compact Date Format:**

```java
// Format to MMddyyyy
String compact = DateTimeFormatUtility.formatToMMDDYYYY(LocalDate.of(2025, 10, 16));
// Returns: "10162025"

// Parse from MMddyyyy
LocalDate date = DateTimeFormatUtility.parseMMDDYYYY("10162025");
```

**Timezone Conversion:**

```java
// Convert UTC to PST
LocalDateTime pstTime = DateTimeFormatUtility.convertUtcToPst(utcDateTime);

// Convert PST to UTC
LocalDateTime utcTime = DateTimeFormatUtility.convertPstToUtc(pstDateTime);
```

#### Available Constants

```java
// Formatters
DateTimeFormatUtility.SIMPLE_DATE_FORMATTER          // yyyy-MM-dd
DateTimeFormatUtility.TIMESTAMP_MILLISECONDS_FORMATTER // yyyy-MM-dd HH:mm:ss.SSS
DateTimeFormatUtility.TIMESTAMP_MICROSECONDS_FORMATTER // yyyy-MM-dd HH:mm:ss.SSSSSS
DateTimeFormatUtility.MMDDYYYY_FORMATTER              // MMddyyyy

// Time Zones
DateTimeFormatUtility.UTC_ZONE  // UTC
DateTimeFormatUtility.PST_ZONE  // America/Los_Angeles
```

#### Important Notes

- All persisted dates should use the format `yyyy-MM-dd` (Simple Date format)
- LocalDateTime is used for Aurora PostgreSQL entity compatibility
- Timestamp formatters default to UTC timezone
- Use Simple Date format for all database date columns

<div style="text-align: right" align="right"><a href="#data-integration-commons-library">Back to Top</a></div>

---

### StringFormatUtility

The StringFormatUtility provides string formatting for numeric codes that require 4-digit format with leading zeros.

#### Usage Examples

**Format Department Code, Fee Code, or Store Number:**

```java
// Format to 4 digits with leading zeros
String deptCode = StringFormatUtility.toFourDigitFormat("123");
// Returns: "0123"

String feeCode = StringFormatUtility.toFourDigitFormat("1");
// Returns: "0001"

String store = StringFormatUtility.toFourDigitFormat("1234");
// Returns: "1234" (already 4 digits)
```

**Validation:**

```java
// Check if string is numeric
boolean isNumeric = StringFormatUtility.isNumeric("1234");
// Returns: true

// Check if string is exactly 4 digits
boolean isFourDigits = StringFormatUtility.isFourDigits("0123");
// Returns: true

// Common pattern: format only if not already 4 digits
String store = "5";
if (!StringFormatUtility.isFourDigits(store)) {
store = StringFormatUtility.toFourDigitFormat(store);
}
// Result: "0005"
```

#### Important Notes

- Department codes must be formatted to 4 digits with leading zeros
- Fee codes must be formatted to 4 digits with leading zeros
- Non-numeric strings are returned as-is without formatting
- Empty strings are returned as-is
- Strings longer than 4 digits are returned as-is

<div style="text-align: right" align="right"><a href="#data-integration-commons-library">Back to Top</a></div>