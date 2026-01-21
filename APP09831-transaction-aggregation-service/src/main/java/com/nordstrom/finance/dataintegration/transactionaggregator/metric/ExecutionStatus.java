package com.nordstrom.finance.dataintegration.transactionaggregator.metric;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Enum represents Lambda Handler Execution Status model. */
@Getter
@RequiredArgsConstructor
public enum ExecutionStatus {
  Success,
  Failure;
}
