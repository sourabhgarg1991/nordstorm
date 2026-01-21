package com.nordstrom.finance.dataintegration.consumer;

import static com.nordstrom.finance.dataintegration.common.util.DateTimeFormatUtility.formatToTimestampMilliseconds;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.*;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.EVENT_TYPE;

import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.KafkaNonRetryableException;
import com.nordstrom.finance.dataintegration.exception.KafkaRetryableException;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortknoxException;
import com.nordstrom.finance.dataintegration.mapper.FinancialRetailTransactionMapper;
import com.nordstrom.finance.dataintegration.metric.Metric;
import com.nordstrom.finance.dataintegration.metric.MetricErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/** Service to consume and process SDM Marketplace events from Kafka. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SDMMarketplaceConsumerService {

  // Service and repository dependencies
  private final TransactionDBService transactionDBService;
  private final FinancialRetailTransactionMapper financialRetailTransactionMapper;
  private final MetricsClient metricsClient;

  /**
   * Kafka listener for SDM Marketplace events.
   *
   * @param sdmFinancialRetailTransaction the incoming transaction payload
   * @param offset the Kafka offset
   * @param partition the Kafka partition
   */
  @KafkaListener(
      id = "retail-listener",
      topics = "${app.kafka.consumer.topic.marketplace}",
      groupId = "${spring.kafka.consumer.group-id}-marketplace")
  public void consumeSDMEvents(
      @Payload final FinancialRetailTransaction sdmFinancialRetailTransaction,
      @Header(value = KafkaHeaders.OFFSET, required = false) long offset,
      @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) int partition) {
    long startTime = System.currentTimeMillis();
    try {
      setLogContext(
          sdmFinancialRetailTransaction, String.valueOf(offset), String.valueOf(partition));
      log.info(
          "Started consuming marketplace event at {}",
          formatToTimestampMilliseconds(LocalDateTime.now()));
      boolean isProcessed = processMarketplaceTransaction(sdmFinancialRetailTransaction);
      if (isProcessed) {
        log.info("Successfully completed marketplace transaction event processing");
        metricsClient.incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
      }
    } catch (KafkaRetryableException | KafkaNonRetryableException e) {
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SDM_MARKETPLACE_EVENTS_CONSUMPTION_ERROR.getErrorValue())));
      throw e;
    } catch (Exception e) {
      log.error("Unexpected exception in marketplace consumer listener. Sending to DLT.", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SDM_MARKETPLACE_EVENTS_CONSUMPTION_ERROR.getErrorValue())));
      throw new KafkaNonRetryableException("Unexpected listener error: " + e.getMessage());
    } finally {
      log.info(
          "Total time taken to process consumed marketplace event: {} ms",
          System.currentTimeMillis() - startTime);
      metricsClient.recordExecutionTime(
          Metric.PROCESSOR_EXECUTION_TIME.getMetricName(), System.currentTimeMillis() - startTime);
    }
  }

  /**
   * Processes a single SDM retail transaction. Checks for duplicates, maps and saves Transaction,
   * TransactionLine, and MarketplaceTransactionLine entities.
   */
  public boolean processMarketplaceTransaction(
      FinancialRetailTransaction financialRetailTransaction) throws FortknoxException {

    String transactionId =
        String.valueOf(financialRetailTransaction.getFinancialRetailTransactionRecordId());

    if (transactionDBService.existsByTransactionId(
        SOURCE_REFERENCE_SYSTEM_TYPE_SDM, SOURCE_REFERENCE_TYPE_MARKETPLACE, transactionId)) {
      log.info("Transaction already exists. Skipping save.");
      metricsClient.incrementCounter(
          Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(),
          EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
      return false;
    }

    try {
      log.info("started mapping Transaction");
      Transaction transaction =
          financialRetailTransactionMapper.toTransaction(financialRetailTransaction);

      transactionDBService.saveTransaction(transaction);
      log.info("Successfully persisted Transaction with line and marketplace line details.");

    } catch (DatabaseConnectionException e) {
      log.error("Error processing transactionId:{}, Error: {}", transactionId, e.getMessage());
      throw new KafkaRetryableException(
          "Error processing Kafka message. Retryable issue occurred: " + e.getMessage());
    } catch (Exception e) {
      log.error("Failed to process transactionId:{}, Error: {}", transactionId, e.getMessage(), e);
      throw new KafkaNonRetryableException(
          "Error processing Kafka message. Non-retryable issue occurred: " + e.getMessage());
    }
    return true;
  }

  /** Sets logging context for MDC. */
  private void setLogContext(FinancialRetailTransaction tx, String offset, String partition) {
    MDC.put("SOURCE_REFERENCE_TYPE", SOURCE_REFERENCE_TYPE_MARKETPLACE);
    MDC.put("SDM_ID", String.valueOf(tx.getFinancialRetailTransactionRecordId()));
    MDC.put("OFFSET", offset);
    MDC.put("PARTITION", partition);
  }
}
