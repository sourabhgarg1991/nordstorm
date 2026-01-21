package com.nordstrom.finance.dataintegration.promotion.database.gcp.service;

import com.google.cloud.bigquery.*;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.common.util.DateTimeFormatUtility;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.config.GcpProperties;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.constant.GcpQueryConstants;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageRequest;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.dto.TransactionPageResponse;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.PromotionGroupType;
import com.nordstrom.finance.dataintegration.promotion.domain.mapper.BigQueryRowToTransactionDetailMapper;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import com.nordstrom.finance.dataintegration.promotion.exception.PromotionQueryException;
import com.nordstrom.finance.dataintegration.promotion.metric.MetricsErrorCode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Service for querying promotion transaction data from Google BigQuery. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionGcpQueryService {
  private final BigQuery bigQuery;
  private final GcpProperties gcpProperties;
  private final MetricsClient metricsClient;

  /**
   * Queries BigQuery for promotion data and returns a page of transaction details.
   *
   * @param transactionPageRequest the request containing job and page token
   * @return a response containing the current page of transaction data and the next page token
   */
  public TransactionPageResponse getPromotionTransactionData(
      TransactionPageRequest transactionPageRequest) throws Exception {

    try {
      Job queryJob =
          transactionPageRequest.job() == null ? createQueryJob() : transactionPageRequest.job();
      TableResult result =
          queryJob.getQueryResults(queryResultsOptions(transactionPageRequest.currentPageToken()));

      Schema schema = result.getSchema();

      List<TransactionDetailVO> transactionDetailList =
          StreamSupport.stream(result.getValues().spliterator(), false)
              .map(
                  row ->
                      BigQueryRowToTransactionDetailMapper.map(
                          FieldValueList.of(row, schema.getFields()),
                          schema)) // Pass schema to mapper
              .toList();

      return new TransactionPageResponse(
          queryJob, transactionDetailList, result.getNextPageToken());
    } catch (Exception e) {
      log.error("Error mapping query results to TransactionDetail", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.GCP_MAPPING_ERROR.name()));
      throw new PromotionQueryException(
          "Error mapping query results to TransactionDetail: " + e.getMessage());
    }
  }

  private Job createQueryJob() throws PromotionQueryException {
    QueryJobConfiguration queryConfig = getQueryJobConfiguration();
    try {
      JobId jobId = JobId.of(gcpProperties.projectId(), UUID.randomUUID().toString());
      log.info(
          "Started fetching promotion transaction data from GCP Query jobId: {}.", jobId.getJob());
      Job queryJob = bigQuery.create(JobInfo.of(jobId, queryConfig)).waitFor();
      if (queryJob == null) {
        log.error("GCP Query job not found");
        metricsClient.incrementErrorCount(
            MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.GCP_QUERY_JOB_CREATE_ERROR.name()));
        throw new PromotionQueryException("Query job not found");
      }
      if (queryJob.getStatus().getError() != null) {
        log.error("GCP Query job encountered an error: {}", queryJob.getStatus().getError());
        metricsClient.incrementErrorCount(
            MetricsCommonTag.ERROR_CODE.getTag(MetricsErrorCode.GCP_QUERY_ERROR.name()));
        throw new PromotionQueryException(
            "GCP Query job encountered an error: " + queryJob.getStatus().getError());
      }
      return queryJob;
    } catch (Exception e) {
      log.error("Error while creating or waiting for QueryJob: {}", e.getMessage());
      throw new PromotionQueryException("Error while creating or waiting for QueryJob.");
    }
  }

  private QueryJobConfiguration getQueryJobConfiguration() {
    String promoQuery =
        String.format(
            GcpQueryConstants.PROMOTION_FETCH_QUERY_WITH_PLACEHOLDERS,
            gcpProperties.napProjectId(),
            gcpProperties.dataset(),
            gcpProperties.tableName());
    return QueryJobConfiguration.newBuilder(promoQuery)
        .addNamedParameter(
            GcpQueryConstants.PARAM_BUSINESS_ORIGIN,
            QueryParameterValue.array(
                new String[] {
                  PromotionGroupType.LOYALTY_PROMO.name(), PromotionGroupType.MARKETING_PROMO.name()
                },
                String.class))
        .addNamedParameter(
            GcpQueryConstants.PARAM_START_DATE,
            QueryParameterValue.dateTime(
                DateTimeFormatUtility.formatToTimestampMicroseconds(
                    parseOrDefault(
                        gcpProperties.startDateOverride(),
                        () -> Instant.now().truncatedTo(ChronoUnit.DAYS)))))
        .addNamedParameter(
            GcpQueryConstants.PARAM_END_DATE,
            QueryParameterValue.dateTime(
                DateTimeFormatUtility.formatToTimestampMicroseconds(
                    parseOrDefault(
                        gcpProperties.endDateOverride(),
                        () ->
                            Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)))))
        .build();
  }

  private static Instant parseOrDefault(
      String value, java.util.function.Supplier<Instant> defaultSupplier) {
    try {
      Instant parsed = Instant.parse(value);
      return parsed.equals(Instant.EPOCH) ? defaultSupplier.get() : parsed;
    } catch (DateTimeParseException e) {
      return defaultSupplier.get();
    }
  }

  private BigQuery.QueryResultsOption[] queryResultsOptions(String currentPageToken) {
    return StringUtils.hasText(currentPageToken)
        ? new BigQuery.QueryResultsOption[] {
          BigQuery.QueryResultsOption.pageSize(gcpProperties.pageSize()),
          BigQuery.QueryResultsOption.pageToken(currentPageToken)
        }
        : new BigQuery.QueryResultsOption[] {
          BigQuery.QueryResultsOption.pageSize(gcpProperties.pageSize())
        };
  }
}
