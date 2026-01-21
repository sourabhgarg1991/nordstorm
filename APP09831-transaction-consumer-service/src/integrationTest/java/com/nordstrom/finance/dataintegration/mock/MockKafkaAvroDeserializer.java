package com.nordstrom.finance.dataintegration.mock;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Mock Kafka Avro Deserializer for integration tests. Uses the shared MockSchemaRegistryClient to
 * deserialize Avro messages in embedded Kafka tests.
 */
public class MockKafkaAvroDeserializer implements Deserializer<Object> {

  private KafkaAvroDeserializer inner;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    Map<String, Object> effectiveConfigs = new HashMap<>(configs);
    effectiveConfigs.put("schema.registry.url", "mock://test-schema-registry");
    effectiveConfigs.put("specific.avro.reader", true);

    // Use the same MockSchemaRegistryClient instance as the serializer
    SchemaRegistryClient schemaRegistry = MockKafkaAvroSerializer.getSchemaRegistryClient();
    inner = new KafkaAvroDeserializer(schemaRegistry);
    inner.configure(effectiveConfigs, isKey);
  }

  @Override
  public Object deserialize(String topic, byte[] data) {
    return inner.deserialize(topic, data);
  }

  @Override
  public Object deserialize(String topic, Headers headers, byte[] data) {
    return inner.deserialize(topic, headers, data);
  }

  @Override
  public void close() {
    if (inner != null) {
      inner.close();
    }
  }
}
