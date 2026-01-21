package com.nordstrom.finance.dataintegration.readiness;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;

@Slf4j
public class CustomReadinessCheckHealthIndicator extends ReadinessStateHealthIndicator {

  private final ReadinessState readinessState;

  public CustomReadinessCheckHealthIndicator(
      ApplicationAvailability availability, ExternalServiceReadinessChecker service) {
    super(availability);
    readinessState =
        service.isServiceReady()
            ? ReadinessState.ACCEPTING_TRAFFIC
            : ReadinessState.REFUSING_TRAFFIC;
  }

  @Override
  protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
    return readinessState;
  }
}
