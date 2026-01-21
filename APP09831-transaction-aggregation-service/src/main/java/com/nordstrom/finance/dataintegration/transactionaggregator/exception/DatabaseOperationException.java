package com.nordstrom.finance.dataintegration.transactionaggregator.exception;

/** Represents errors related to Database Operation exceptions */
public class DatabaseOperationException extends Exception {
  public DatabaseOperationException(String message) {
    super(message);
  }
}
