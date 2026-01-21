package com.nordstrom.finance.dataintegration.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DatabaseOperationExceptionTest {

  @Test
  void testMessageConstructor() {
    String errorMessage = "Database operation failed";
    DatabaseOperationException exception = new DatabaseOperationException(errorMessage);

    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause(), "Cause should be null when only message is provided");
  }

  @Test
  void testMessageAndCauseConstructor() {
    String errorMessage = "Database operation failed with cause";
    Throwable cause = new RuntimeException("Underlying DB error");

    DatabaseOperationException exception = new DatabaseOperationException(errorMessage, cause);

    assertEquals(errorMessage, exception.getMessage());
    assertEquals(cause, exception.getCause(), "Cause should be preserved");
  }
}
