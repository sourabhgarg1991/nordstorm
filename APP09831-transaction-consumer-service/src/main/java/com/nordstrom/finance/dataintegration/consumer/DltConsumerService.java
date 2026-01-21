package com.nordstrom.finance.dataintegration.consumer;

import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_MARKETPLACE;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_RESTAURANT;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.EVENT_TYPE;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.metric.Metric;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service("dltConsumerService")
@RequiredArgsConstructor
public class DltConsumerService {

  private final SDMRestaurantConsumerService sdmRestaurantConsumerService;
  private final SDMMarketplaceConsumerService sdmMarketplaceConsumerService;
  private final MetricsClient metricsClient;

  @Value("${spring.kafka.consumer.properties.schema.registry.url}")
  private String schemaRegistryUrl;

  /**
   * DLT Handler for Restaurant transactions that failed after all retries. This method is
   * registered as DLT Handler in KafkaRetryTopicConfig for restaurant topic.
   *
   * @param record SDM byte[] from DLT topic
   * @param originalException the original exception class name
   * @param offset the Kafka offset
   * @param partition the Kafka partition
   * @param originalMessage the original exception message
   */
  public void processRestaurantDltMessage(
      ConsumerRecord<String, byte[]> record,
      @Header(KafkaHeaders.EXCEPTION_FQCN) String originalException,
      @Header(value = KafkaHeaders.OFFSET, required = false) String offset,
      @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) String partition,
      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String originalMessage) {

    try {
      log.warn(
          "Processing DLT message for restaurant transaction. Topic: {}, Offset: {}, Partition: {}",
          record.topic(),
          offset,
          partition);

      KafkaAvroDeserializer deserializer = createConfiguredDeserializer();
      FinancialRestaurantTransaction transaction =
          (FinancialRestaurantTransaction)
              deserializer.deserialize(
                  record.topic(), record.value(), FinancialRestaurantTransaction.getClassSchema());

      log.info("Attempting to reprocess restaurant transaction from DLT");
      boolean isProcessed = sdmRestaurantConsumerService.processRestaurantTransaction(transaction);
      if (isProcessed) {
        log.info("Successfully reprocessed restaurant transaction from DLT");
        metricsClient.incrementCounter(
            Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
      }
    } catch (Exception reprocessingException) {
      log.error(
          "Failed to reprocess restaurant transaction from DLT. Transaction will remain in DLT. [originalException: {}, originalMessage: {}, reprocessingException: {}, offset: {}]",
          originalException,
          originalMessage,
          reprocessingException.getMessage(),
          offset,
          reprocessingException);
    }
  }

  /**
   * DLT Handler for Marketplace transactions that failed after all retries. This method is
   * registered as DLT Handler in KafkaRetryTopicConfig for marketplace topic.
   *
   * @param record SDM byte[] from DLT topic
   * @param originalException the original exception class name
   * @param offset the Kafka offset
   * @param partition the Kafka partition
   * @param originalMessage the original exception message
   */
  public void processMarketplaceDltMessage(
      ConsumerRecord<String, byte[]> record,
      @Header(KafkaHeaders.EXCEPTION_FQCN) String originalException,
      @Header(value = KafkaHeaders.OFFSET, required = false) String offset,
      @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) String partition,
      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String originalMessage) {

    try {
      log.warn(
          "Processing DLT message for marketplace transaction. Topic: {}, Offset: {}, Partition: {}",
          record.topic(),
          offset,
          partition);

      KafkaAvroDeserializer deserializer = createConfiguredDeserializer();
      FinancialRetailTransaction transaction =
          (FinancialRetailTransaction)
              deserializer.deserialize(
                  record.topic(), record.value(), FinancialRetailTransaction.getClassSchema());

      log.info("Attempting to reprocess marketplace transaction from DLT");
      boolean isProcessed =
          sdmMarketplaceConsumerService.processMarketplaceTransaction(transaction);
      if (isProcessed) {
        log.info("Successfully reprocessed marketplace transaction from DLT");
        metricsClient.incrementCounter(
            Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
            EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
      }

    } catch (Exception reprocessingException) {
      log.error(
          "Failed to reprocess marketplace transaction from DLT. Transaction will remain in DLT. [originalException: {}, originalMessage: {}, reprocessingException: {}, offset: {}]",
          originalException,
          originalMessage,
          reprocessingException.getMessage(),
          offset,
          reprocessingException);
    }
  }

  private KafkaAvroDeserializer createConfiguredDeserializer() {
    KafkaAvroDeserializer deserializer = new KafkaAvroDeserializer();
    deserializer.configure(
        Map.of("schema.registry.url", schemaRegistryUrl, "specific.avro.reader", true), false);
    return deserializer;
  }
}
