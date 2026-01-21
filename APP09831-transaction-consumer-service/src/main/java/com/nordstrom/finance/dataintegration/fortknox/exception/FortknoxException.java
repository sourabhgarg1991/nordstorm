package com.nordstrom.finance.dataintegration.fortknox.exception;

/** Represents error related to API operations with Fortknox Service. */
public class FortknoxException extends Exception {

  /**
   * Constructs a new FortknoxException with the specified detailed message.
   *
   * @param message The detail message describing the specific error.
   */
  public FortknoxException(String message) {
    super(message);
  }
}
