package com.nordstrom.finance.dataintegration.ertm.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatabaseOperationExceptionTest {

  @Test
  void testMessageConstructor() {
    DatabaseOperationException ex = new DatabaseOperationException("Operation error");
    assertEquals("Operation error", ex.getMessage());
  }

  @Test
  void testMessageAndCauseConstructor() {
    Throwable cause = new RuntimeException("Root cause");
    DatabaseOperationException ex = new DatabaseOperationException("Operation error", cause);
    assertEquals("Operation error", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }
}
