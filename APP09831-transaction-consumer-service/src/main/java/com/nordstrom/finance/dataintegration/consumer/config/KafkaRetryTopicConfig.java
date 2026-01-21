package com.nordstrom.finance.dataintegration.consumer.config;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.exception.KafkaNonRetryableException;
import com.nordstrom.finance.dataintegration.exception.KafkaRetryableException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

@EnableKafka
@EnableScheduling
@Configuration
public class KafkaRetryTopicConfig extends RetryTopicConfigurationSupport {
  @Value("${spring.kafka.retry.topic.fixed-delay}")
  private int fixBackoffPeriod;

  @Value("${app.kafka.retry.topic.suffix}")
  private String retrySuffix;

  @Value("${app.kafka.dlt.topic.suffix}")
  private String dltSuffix;

  @Value("${app.kafka.consumer.topic.restaurant}")
  private String restaurantTopic;

  @Value("${app.kafka.consumer.topic.marketplace}")
  private String marketplaceTopic;

  @Autowired private KafkaProperties kafkaProperties;

  @Bean
  public ProducerFactory<String, Object> kafkaProducerFactory() {
    ProducerFactory<String, Object> factory =
        new DefaultKafkaProducerFactory<>(
            kafkaProperties.buildProducerProperties(null),
            new StringSerializer(),
            new DelegatingByTypeSerializer(
                Map.of(
                    byte[].class,
                    new ByteArraySerializer(),
                    FinancialRestaurantTransaction.class,
                    new KafkaAvroSerializer(),
                    FinancialRetailTransaction.class,
                    new KafkaAvroSerializer())));
    return factory;
  }

  @Bean
  @ConditionalOnMissingBean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(kafkaProducerFactory());
  }

  @Override
  protected void configureBlockingRetries(BlockingRetriesConfigurer blockingRetries) {
    blockingRetries
        .retryOn(KafkaRetryableException.class, ListenerExecutionFailedException.class)
        .backOff(new FixedBackOff(fixBackoffPeriod, 3));
  }

  @Override
  protected void manageNonBlockingFatalExceptions(
      List<Class<? extends Throwable>> nonBlockingFatalExceptions) {
    nonBlockingFatalExceptions.add(KafkaNonRetryableException.class);
  }

  @Bean
  public RetryTopicConfiguration restaurantTransactionRetryTopic(
      KafkaTemplate<String, Object> template) {
    return RetryTopicConfigurationBuilder.newInstance()
        .includeTopics(List.of(restaurantTopic))
        .fixedBackOff(fixBackoffPeriod)
        .maxAttempts(3)
        .useSingleTopicForSameIntervals()
        .notRetryOn(KafkaNonRetryableException.class)
        .retryTopicSuffix(retrySuffix)
        .dltHandlerMethod("dltConsumerService", "processRestaurantDltMessage")
        .dltSuffix(dltSuffix)
        .autoCreateTopics(false, 1, (short) 2)
        .create(template);
  }

  @Bean
  public RetryTopicConfiguration marketplaceTransactionRetryTopic(
      KafkaTemplate<String, Object> template) {
    return RetryTopicConfigurationBuilder.newInstance()
        .includeTopics(List.of(marketplaceTopic))
        .fixedBackOff(fixBackoffPeriod)
        .maxAttempts(3)
        .useSingleTopicForSameIntervals()
        .notRetryOn(KafkaNonRetryableException.class)
        .retryTopicSuffix(retrySuffix)
        .dltHandlerMethod("dltConsumerService", "processMarketplaceDltMessage")
        .dltSuffix(dltSuffix)
        .autoCreateTopics(false, 1, (short) 2)
        .create(template);
  }

  @Override
  protected void configureCustomizers(CustomizersConfigurer configurer) {
    Properties props = new Properties();
    props.setProperty(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    configurer.customizeListenerContainer(
        container -> {
          if (Objects.requireNonNull(container.getContainerProperties().getTopics())[0].endsWith(
              "-dlt")) {
            container.getContainerProperties().setKafkaConsumerProperties(props);
          }
        });
  }
}
