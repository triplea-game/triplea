package org.triplea.lobby.server.controller.rest.exception;

public class InsufficientRightsException extends RuntimeException {
  private static final long serialVersionUID = -7034723918553851436L;

  public InsufficientRightsException(final String message) {
    super(message);
  }
}
