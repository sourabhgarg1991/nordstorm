package com.nordstrom.finance.dataintegration.ertm.exception;

public class DatabaseOperationException extends RuntimeException {
  public DatabaseOperationException(String message) {
    super(message);
  }

  public DatabaseOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
