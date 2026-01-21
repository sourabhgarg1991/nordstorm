package com.nordstrom.finance.dataintegration.ertm.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatabaseConnectionExceptionTest {

  @Test
  void testMessageConstructor() {
    DatabaseConnectionException ex = new DatabaseConnectionException("Connection error");
    assertEquals("Connection error", ex.getMessage());
  }

  @Test
  void testMessageAndCauseConstructor() {
    Throwable cause = new RuntimeException("Root cause");
    DatabaseConnectionException ex = new DatabaseConnectionException("Connection error", cause);
    assertEquals("Connection error", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }
}
