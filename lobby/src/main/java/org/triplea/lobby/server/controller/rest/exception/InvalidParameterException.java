package org.triplea.lobby.server.controller.rest.exception;

/**
 * An unchecked Exception indicating that the server
 * expected a parameter of any kind that wasn't provided by the client.
 */
public class InvalidParameterException extends RuntimeException {

  private static final long serialVersionUID = -6215512355547003317L;

  public InvalidParameterException(final String message) {
    super(message);
  }
}
