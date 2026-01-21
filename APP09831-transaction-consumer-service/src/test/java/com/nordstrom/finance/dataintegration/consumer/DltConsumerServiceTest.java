package com.nordstrom.finance.dataintegration.consumer;

import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_MARKETPLACE;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_RESTAURANT;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.EVENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortknoxException;
import com.nordstrom.finance.dataintegration.metric.Metric;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.lang.reflect.Field;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DltConsumerServiceTest {

  private final String restaurantDltTopic =
      "customer-financial-restaurant-transaction-operational-avro-data-integration-dlt";
  private final String marketplaceDltTopic =
      "customer-financial-retail-transaction-operational-avro-data-integration-dlt";

  @Mock private MetricsClient metricsClient;
  @Mock private SDMRestaurantConsumerService sdmRestaurantConsumerService;
  @Mock private SDMMarketplaceConsumerService sdmMarketplaceConsumerService;
  @InjectMocks private DltConsumerService dltService;

  @BeforeEach
  void setup() {
    try {
      Field field = DltConsumerService.class.getDeclaredField("schemaRegistryUrl");
      field.setAccessible(true);
      field.set(dltService, "http://mock-schema-registry:8081");
    } catch (Exception e) {
      fail("Failed to inject schemaRegistryUrl: " + e.getMessage());
    }
  }

  // ========== SDM Restaurant DLT Tests ==========

  @Test
  void testProcessRestaurantDltMessage_successfulReprocessing() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-123");

    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenReturn(tx);
            })) {

      when(sdmRestaurantConsumerService.processRestaurantTransaction(any())).thenReturn(true);

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(restaurantDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processRestaurantDltMessage(
                  record, "ExceptionClass", "1", "0", "OriginalMessage"));

      verify(sdmRestaurantConsumerService, times(1)).processRestaurantTransaction(tx);
      verify(metricsClient, times(1))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
    }
  }

  @Test
  void testProcessRestaurantDltMessage_deserializationError() {
    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenThrow(new RuntimeException("Deserialization failed"));
            })) {

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(restaurantDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processRestaurantDltMessage(
                  record, "ExceptionClass", "1", "0", "OriginalMessage"));

      verify(sdmRestaurantConsumerService, never()).processRestaurantTransaction(any());
      verify(metricsClient, times(0))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
    }
  }

  @Test
  void testProcessRestaurantDltMessage_processingError() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-123");

    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenReturn(tx);
            })) {

      doThrow(new RuntimeException("Processing failed"))
          .when(sdmRestaurantConsumerService)
          .processRestaurantTransaction(any());

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(restaurantDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processRestaurantDltMessage(
                  record, "ExceptionClass", "1", "0", "OriginalMessage"));

      verify(metricsClient, times(0))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
    }
  }

  @Test
  void testProcessRestaurantDltMessage_withNullHeaders() {
    FinancialRestaurantTransaction tx = mock(FinancialRestaurantTransaction.class);
    when(tx.getFinancialRestaurantTransactionRecordId()).thenReturn("id-456");

    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenReturn(tx);
            })) {

      when(sdmRestaurantConsumerService.processRestaurantTransaction(any())).thenReturn(true);

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(restaurantDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processRestaurantDltMessage(
                  record, "ExceptionClass", null, null, "OriginalMessage"));

      verify(sdmRestaurantConsumerService, times(1)).processRestaurantTransaction(tx);
      verify(metricsClient, times(1))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_RESTAURANT));
    }
  }

  // ========== SDM Marketplace DLT Tests ==========

  @Test
  void testProcessMarketplaceDltMessage_successfulReprocessing() throws FortknoxException {
    FinancialRetailTransaction tx = mock(FinancialRetailTransaction.class);
    when(tx.getFinancialRetailTransactionRecordId()).thenReturn("id-789");

    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenReturn(tx);
            })) {

      when(sdmMarketplaceConsumerService.processMarketplaceTransaction(any())).thenReturn(true);

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(marketplaceDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processMarketplaceDltMessage(
                  record, "ExceptionClass", "1", "0", "OriginalMessage"));

      verify(sdmMarketplaceConsumerService, times(1)).processMarketplaceTransaction(tx);
      verify(metricsClient, times(1))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
    }
  }

  @Test
  void testProcessMarketplaceDltMessage_deserializationError() throws FortknoxException {
    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenThrow(new RuntimeException("Deserialization failed"));
            })) {

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(marketplaceDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processMarketplaceDltMessage(
                  record, "ExceptionClass", "1", "0", "OriginalMessage"));

      verify(sdmMarketplaceConsumerService, never()).processMarketplaceTransaction(any());
      verify(metricsClient, times(0))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
    }
  }

  @Test
  void testProcessMarketplaceDltMessage_processingError() throws FortknoxException {
    FinancialRetailTransaction tx = mock(FinancialRetailTransaction.class);
    when(tx.getFinancialRetailTransactionRecordId()).thenReturn("id-999");

    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenReturn(tx);
            })) {

      doThrow(new RuntimeException("Processing failed"))
          .when(sdmMarketplaceConsumerService)
          .processMarketplaceTransaction(any());

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(marketplaceDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processMarketplaceDltMessage(
                  record, "ExceptionClass", "1", "0", "OriginalMessage"));

      verify(sdmMarketplaceConsumerService, times(1)).processMarketplaceTransaction(tx);
      verify(metricsClient, times(0))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
    }
  }

  @Test
  void testProcessMarketplaceDltMessage_withNullHeaders() throws FortknoxException {
    FinancialRetailTransaction tx = mock(FinancialRetailTransaction.class);
    when(tx.getFinancialRetailTransactionRecordId()).thenReturn("id-888");

    try (MockedConstruction<KafkaAvroDeserializer> ignored =
        mockConstruction(
            KafkaAvroDeserializer.class,
            (mock, context) -> {
              doNothing().when(mock).configure(any(), anyBoolean());
              when(mock.deserialize(anyString(), any(byte[].class), any(Schema.class)))
                  .thenReturn(tx);
            })) {

      when(sdmMarketplaceConsumerService.processMarketplaceTransaction(any())).thenReturn(true);

      ConsumerRecord<String, byte[]> record =
          new ConsumerRecord<>(marketplaceDltTopic, 0, 0L, "key", new byte[] {1, 2, 3});

      assertDoesNotThrow(
          () ->
              dltService.processMarketplaceDltMessage(
                  record, "ExceptionClass", null, null, "OriginalMessage"));

      verify(sdmMarketplaceConsumerService, times(1)).processMarketplaceTransaction(tx);
      verify(metricsClient, times(1))
          .incrementCounter(
              Metric.DLT_EVENTS_RECOVERED_COUNT.getMetricName(),
              EVENT_TYPE.getTag(SOURCE_REFERENCE_TYPE_MARKETPLACE));
    }
  }
}
