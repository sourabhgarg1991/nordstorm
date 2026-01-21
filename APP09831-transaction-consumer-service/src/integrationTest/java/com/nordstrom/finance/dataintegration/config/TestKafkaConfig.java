package com.nordstrom.finance.dataintegration.config;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.mock.MockKafkaAvroSerializer;
import java.util.Map;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;

/**
 * Test Kafka Configuration for Integration Tests. Uses MockKafkaAvroSerializer to bypass schema
 * registry requirements.
 */
@TestConfiguration
public class TestKafkaConfig {

  @Autowired private KafkaProperties kafkaProperties;

  @Bean(name = "kafkaTestTemplate")
  @Primary
  public KafkaTemplate<String, Object> kafkaTestTemplate() {
    ProducerFactory<String, Object> factory =
        new DefaultKafkaProducerFactory<>(
            kafkaProperties.buildProducerProperties(null),
            new StringSerializer(),
            new DelegatingByTypeSerializer(
                Map.of(
                    byte[].class,
                    new ByteArraySerializer(),
                    FinancialRestaurantTransaction.class,
                    new MockKafkaAvroSerializer(),
                    FinancialRetailTransaction.class,
                    new MockKafkaAvroSerializer())));
    return new KafkaTemplate<>(factory);
  }
}
