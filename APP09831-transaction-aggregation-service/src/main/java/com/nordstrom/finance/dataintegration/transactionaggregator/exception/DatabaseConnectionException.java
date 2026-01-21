package com.nordstrom.finance.dataintegration.transactionaggregator.exception;

/** Represents errors related to Database Connection and Query Timeout exceptions */
public class DatabaseConnectionException extends Exception {

  public DatabaseConnectionException(String message) {
    super(message);
  }

  public static DatabaseConnectionException toAuroraDB() {
    return new DatabaseConnectionException("Failed to connect to Aurora DB");
  }
}
