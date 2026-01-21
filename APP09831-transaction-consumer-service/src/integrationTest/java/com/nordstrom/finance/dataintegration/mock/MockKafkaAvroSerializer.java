package com.nordstrom.finance.dataintegration.mock;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Mock Kafka Avro Serializer for integration tests. Uses MockSchemaRegistryClient to bypass real
 * schema registry while maintaining Confluent wire format compatibility for embedded Kafka testing.
 */
public class MockKafkaAvroSerializer implements Serializer<SpecificRecordBase> {

  private KafkaAvroSerializer inner;
  private static final SchemaRegistryClient schemaRegistry = new MockSchemaRegistryClient();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    Map<String, Object> effectiveConfigs = new HashMap<>(configs);
    effectiveConfigs.put("schema.registry.url", "mock://test-schema-registry");
    effectiveConfigs.put("auto.register.schemas", true);

    inner = new KafkaAvroSerializer(schemaRegistry);
    inner.configure(effectiveConfigs, isKey);
  }

  @Override
  public byte[] serialize(String topic, SpecificRecordBase data) {
    return inner.serialize(topic, data);
  }

  @Override
  public byte[] serialize(String topic, Headers headers, SpecificRecordBase data) {
    return inner.serialize(topic, headers, data);
  }

  @Override
  public void close() {
    if (inner != null) {
      inner.close();
    }
  }

  /**
   * Get the shared MockSchemaRegistryClient instance for testing.
   *
   * @return the mock schema registry client
   */
  public static SchemaRegistryClient getSchemaRegistryClient() {
    return schemaRegistry;
  }
}
