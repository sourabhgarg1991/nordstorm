package com.nordstrom.finance.dataintegration.promotion.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PromotionQueryExceptionTest {

  @Test
  void testMessageConstructor() {
    String errorMessage = "GCP query failed";
    PromotionQueryException exception = new PromotionQueryException(errorMessage);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause(), "Cause should be null when only message is provided");
  }
}
