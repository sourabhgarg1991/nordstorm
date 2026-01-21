package com.nordstrom.finance.dataintegration.promotion.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatabaseConnectionExceptionTest {

  @Test
  void testMessageConstructor() {
    String errorMessage = "Database connection failed";
    DatabaseConnectionException exception = new DatabaseConnectionException(errorMessage);

    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause(), "Cause should be null when only message is provided");
  }

  @Test
  void testMessageAndCauseConstructor() {
    String errorMessage = "Database connection failed with cause";
    Throwable cause = new RuntimeException("Timeout error");

    DatabaseConnectionException exception = new DatabaseConnectionException(errorMessage, cause);

    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause(), "Cause should be preserved");
  }
}
