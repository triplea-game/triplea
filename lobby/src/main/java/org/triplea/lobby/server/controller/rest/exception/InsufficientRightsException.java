package org.triplea.lobby.server.controller.rest.exception;

/**
 * An unchecked Exception indicating that the server could
 * authenticate the client, but the user has insufficient rights
 * to perform the requested operation.
 */
public class InsufficientRightsException extends RuntimeException {
  private static final long serialVersionUID = -7034723918553851436L;

  public InsufficientRightsException(final String message) {
    super(message);
  }
}
