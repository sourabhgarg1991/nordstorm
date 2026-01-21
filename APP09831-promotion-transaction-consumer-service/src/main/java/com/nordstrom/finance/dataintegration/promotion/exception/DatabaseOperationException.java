package com.nordstrom.finance.dataintegration.promotion.exception;

/** Exception representing errors related to database operations. */
public final class DatabaseOperationException extends Exception {

  public DatabaseOperationException(String message) {
    super(message);
  }

  public DatabaseOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
