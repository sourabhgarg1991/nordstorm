package com.nordstrom.finance.dataintegration.exception;

public class ObjectMappingException extends RuntimeException {

  public ObjectMappingException(String message) {
    super(message);
  }

  public ObjectMappingException(String message, Throwable cause) {
    super(message, cause);
  }
}
