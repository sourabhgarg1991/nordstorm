package com.nordstrom.finance.dataintegration.ertm.exception;

/** Represents errors related to DataSourceExtract exceptions */
public class S3ConnectionException extends Exception {
  public S3ConnectionException(String message) {
    super(message);
  }
}
