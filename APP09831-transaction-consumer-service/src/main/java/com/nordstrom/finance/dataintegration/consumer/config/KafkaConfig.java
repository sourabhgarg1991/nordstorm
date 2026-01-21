package com.nordstrom.finance.dataintegration.consumer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Kafka configuration for Avro-based Object consumption. Binds to externalized KafkaProperties for
 * flexible configuration.
 */
@Configuration
@EnableKafka
@EnableRetry
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

  private final KafkaProperties properties;

  @Bean
  public KafkaAdmin kafkaAdmin() {
    return new KafkaAdmin(properties.buildAdminProperties(null));
  }

  /** Listener container factory for Object messages. Configures concurrency based on properties. */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setConcurrency(properties.getListener().getConcurrency());
    return factory;
  }

  /** Consumer factory for Object messages using application Kafka properties. */
  @Bean
  public ConsumerFactory<Object, Object> consumerFactory() {
    return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(null));
  }
}
