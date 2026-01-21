package com.nordstrom.finance.dataintegration.promotion.exception;

/** Exception representing errors related to database connection and query timeouts. */
public final class DatabaseConnectionException extends Exception {

  public DatabaseConnectionException(String message) {
    super(message);
  }

  public DatabaseConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
