package com.nordstrom.finance.dataintegration.ertm.exception;

public class DatabaseConnectionException extends RuntimeException {
  public DatabaseConnectionException(String message) {
    super(message);
  }

  public DatabaseConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
