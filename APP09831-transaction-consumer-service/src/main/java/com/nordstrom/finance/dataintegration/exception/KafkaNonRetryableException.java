package com.nordstrom.finance.dataintegration.exception;

/** Custom exception for all Kafka Non-Retryable errors */
public class KafkaNonRetryableException extends RuntimeException {

  public KafkaNonRetryableException(String message) {
    super(message);
  }
}
