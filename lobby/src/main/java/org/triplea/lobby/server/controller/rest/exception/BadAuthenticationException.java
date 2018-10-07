package org.triplea.lobby.server.controller.rest.exception;

public class BadAuthenticationException extends RuntimeException {

  private static final long serialVersionUID = -2853430130032064791L;

  public BadAuthenticationException(final String message) {
    super(message);
  }
}
