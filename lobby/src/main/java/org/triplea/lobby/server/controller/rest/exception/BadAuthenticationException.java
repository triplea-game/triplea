package org.triplea.lobby.server.controller.rest.exception;

/**
 * An unchecked Exception indicating that the server could
 * not authenticate the client based on the passed information.
 */
public class BadAuthenticationException extends RuntimeException {

  private static final long serialVersionUID = -2853430130032064791L;

  public BadAuthenticationException(final String message) {
    super(message);
  }
}
