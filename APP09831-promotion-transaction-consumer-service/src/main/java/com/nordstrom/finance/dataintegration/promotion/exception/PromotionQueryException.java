package com.nordstrom.finance.dataintegration.promotion.exception;

/** Exception representing errors related to GCP promotion queries (e.g., BigQuery failures). */
public final class PromotionQueryException extends Exception {

  public PromotionQueryException(String message) {
    super(message);
  }
}
