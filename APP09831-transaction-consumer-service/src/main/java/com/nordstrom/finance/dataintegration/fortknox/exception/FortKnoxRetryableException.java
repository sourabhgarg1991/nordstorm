package com.nordstrom.finance.dataintegration.fortknox.exception;

/**
 * Represents retryable error with FortKnox Service. This exception is thrown only for the retry
 * logic when server-related error occurs.
 */
public class FortKnoxRetryableException extends RuntimeException {

  public FortKnoxRetryableException(String message) {
    super(message);
  }
}
