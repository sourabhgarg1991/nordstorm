package com.nordstrom.finance.dataintegration.consumer;

import static com.nordstrom.finance.dataintegration.common.util.DateTimeFormatUtility.formatToTimestampMilliseconds;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.*;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.EVENT_TYPE;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.KafkaNonRetryableException;
import com.nordstrom.finance.dataintegration.exception.KafkaRetryableException;
import com.nordstrom.finance.dataintegration.mapper.FinancialRestaurantTransactionMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SDMRestaurantConsumerService {
  private final TransactionDBService transactionDBService;
  private final FinancialRestaurantTransactionMapper financialRestaurantTransactionMapper;
  private final MetricsClient metricsClient;

  @KafkaListener(
      id = "restaurant-listener",
      topics = "${app.kafka.consumer.topic.restaurant}",
      groupId = "${spring.kafka.consumer.group-id}-restaurant")
  public void consumeSDMRestaurantEvents(
      @Payload final FinancialRestaurantTransaction sdmFinancialRestaurantTransaction,
      @Header(value = KafkaHeaders.OFFSET, required = false) long offset,
      @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) int partition) {
    long startTime = System.currentTimeMillis();
    try {
      setLogContext(
          sdmFinancialRestaurantTransaction, String.valueOf(offset), String.valueOf(partition));
      log.info(
          "Started consuming restaurant event at {}",
          formatToTimestampMilliseconds(LocalDateTime.now()));
      boolean isProcessed = processRestaurantTransaction(sdmFinancialRestaurantTransaction);
      if (isProcessed) {
        log.info("Successfully completed restaurant transaction event processing");
        metricsClient.incrementCounter(
            Metric.SDM_EVENTS_CONSUMED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
      }
    } catch (KafkaRetryableException | KafkaNonRetryableException e) {
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SDM_RESTAURANT_EVENTS_CONSUMPTION_ERROR.getErrorValue())));
      throw e;
    } catch (Exception e) {
      log.error("Unexpected exception in restaurant consumer listener. Sending to DLT.", e);
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.SDM_RESTAURANT_EVENTS_CONSUMPTION_ERROR.getErrorValue())));
      throw new KafkaNonRetryableException("Unexpected listener error: " + e.getMessage());
    } finally {
      long processingTime = System.currentTimeMillis() - startTime;
      log.info(
          "Total time taken to process consumed restaurant transaction event: {} ms",
          processingTime);
      metricsClient.recordExecutionTime(
          Metric.PROCESSOR_EXECUTION_TIME.getMetricName(), processingTime);
    }
  }

  /**
   * Process and persist the SDM restaurant transaction.
   *
   * @param restaurantTransaction the FinancialRestaurantTransaction to process
   */
  public boolean processRestaurantTransaction(
      FinancialRestaurantTransaction restaurantTransaction) {
    String sdmTransactionId =
        String.valueOf(restaurantTransaction.getFinancialRestaurantTransactionRecordId());

    if (transactionDBService.existsByTransactionId(
        SOURCE_REFERENCE_SYSTEM_TYPE_SDM, SOURCE_REFERENCE_TYPE_RESTAURANT, sdmTransactionId)) {
      log.info(
          "Transaction already exists for TransactionId={} . Skipping save.", sdmTransactionId);
      metricsClient.incrementCounter(
          Metric.DUPLICATE_TRANSACTION_COUNT.getMetricName(),
          EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
      return false;
    }

    try {
      log.info("Started Mapping Transaction");
      Transaction transaction =
          financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(
              restaurantTransaction);

      transactionDBService.saveTransaction(transaction);
      log.info("Successfully processed SDM restaurant transaction");

    } catch (DatabaseConnectionException e) {
      log.error("Error processing transactionId:{}, Error: {}", sdmTransactionId, e.getMessage());
      throw new KafkaRetryableException(
          "Error processing Kafka message. Retryable issue occurred: " + e.getMessage());
    } catch (Exception e) {
      log.error(
          "Failed to process transactionId:{}, Error: {}", sdmTransactionId, e.getMessage(), e);
      throw new KafkaNonRetryableException(
          "Error processing Kafka message. Non-retryable issue occurred: " + e.getMessage());
    }
    return true;
  }

  private void setLogContext(FinancialRestaurantTransaction tx, String offset, String partition) {
    MDC.put("SOURCE_REFERENCE_TYPE", SOURCE_REFERENCE_TYPE_RESTAURANT);
    MDC.put("SDM_ID", String.valueOf(tx.getFinancialRestaurantTransactionRecordId()));
    MDC.put("OFFSET", offset);
    MDC.put("PARTITION", partition);
  }
}
