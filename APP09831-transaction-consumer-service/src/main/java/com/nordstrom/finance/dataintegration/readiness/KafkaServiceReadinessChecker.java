package com.nordstrom.finance.dataintegration.readiness;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"!integrationtest & !loadtest"})
public class KafkaServiceReadinessChecker implements ExternalServiceReadinessChecker {
  @Autowired KafkaAdmin adminClient;

  @Value("${app.kafka.consumer.topic.marketplace}")
  String MARKETPLACE_CONSUMER_TOPIC;

  @Value("${app.kafka.consumer.topic.restaurant}")
  String RESTAURANT_CONSUMER_TOPIC;

  @Value("${app.kafka.retry.topic.suffix}")
  String RETRY_TOPIC_SUFFIX;

  @Value("${app.kafka.dlt.topic.suffix}")
  String DLT_TOPIC_SUFFIX;

  @Override
  public boolean isServiceReady() {
    try {
      String marketplaceRetryTopic = MARKETPLACE_CONSUMER_TOPIC + RETRY_TOPIC_SUFFIX;
      String marketplaceDLTTopic = MARKETPLACE_CONSUMER_TOPIC + DLT_TOPIC_SUFFIX;
      String restaurantRetryTopic = RESTAURANT_CONSUMER_TOPIC + RETRY_TOPIC_SUFFIX;
      String restaurantDLTTopic = RESTAURANT_CONSUMER_TOPIC + DLT_TOPIC_SUFFIX;
      Map<String, TopicDescription> existingTopics =
          adminClient.describeTopics(
              MARKETPLACE_CONSUMER_TOPIC,
              marketplaceRetryTopic,
              marketplaceDLTTopic,
              RESTAURANT_CONSUMER_TOPIC,
              restaurantRetryTopic,
              restaurantDLTTopic);
      return existingTopics.get(MARKETPLACE_CONSUMER_TOPIC) != null
          && existingTopics.get(marketplaceRetryTopic) != null
          && existingTopics.get(marketplaceDLTTopic) != null
          && existingTopics.get(RESTAURANT_CONSUMER_TOPIC) != null
          && existingTopics.get(restaurantRetryTopic) != null
          && existingTopics.get(restaurantDLTTopic) != null;
    } catch (Exception e) {
      log.error("Kafka service is not available. Error : {}", e.getMessage());
    }
    return false;
  }
}
