package org.triplea.lobby.server.controller.rest.exception;

public class InvalidParameterException extends RuntimeException {

  private static final long serialVersionUID = -6215512355547003317L;

  public InvalidParameterException(final String message) {
    super(message);
  }
}
