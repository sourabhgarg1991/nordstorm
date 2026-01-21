package com.nordstrom.finance.dataintegration.exception;

/** Custom exception for Blocking Kafka Retryable errors */
public class KafkaRetryableException extends RuntimeException {

  public KafkaRetryableException(String message) {
    super(message);
  }
}
