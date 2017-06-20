package games.strategy.engine.pbem;

final class PasswordManagerException extends Exception {
  private static final long serialVersionUID = -110629801418732489L;

  PasswordManagerException(final String message) {
    super(message);
  }

  PasswordManagerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
