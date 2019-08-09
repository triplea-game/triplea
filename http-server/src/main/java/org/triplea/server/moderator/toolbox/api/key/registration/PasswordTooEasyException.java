package org.triplea.server.moderator.toolbox.api.key.registration;

/** Exception to indicate a provided password given during registration is too easy to guess. */
public class PasswordTooEasyException extends RuntimeException {
  private static final long serialVersionUID = 3863062450866220420L;

  public PasswordTooEasyException() {
    super("Password rejected, too easy to guess.");
  }
}
