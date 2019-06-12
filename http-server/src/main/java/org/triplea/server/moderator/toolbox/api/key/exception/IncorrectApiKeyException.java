package org.triplea.server.moderator.toolbox.api.key.exception;

/**
 * Thrown when we have attempted to validate an API key and it was incorrect.
 */
public class IncorrectApiKeyException extends RuntimeException {
  private static final long serialVersionUID = -4707883233900924830L;

  public IncorrectApiKeyException() {
    super("Invalid Key");
  }
}
