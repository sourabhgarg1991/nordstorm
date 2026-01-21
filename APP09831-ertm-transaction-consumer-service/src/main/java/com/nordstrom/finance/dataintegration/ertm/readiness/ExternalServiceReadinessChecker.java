package com.nordstrom.finance.dataintegration.ertm.readiness;

public interface ExternalServiceReadinessChecker {
  /**
   * @return true if service is ready, false otherwise
   */
  boolean isServiceReady();
}
