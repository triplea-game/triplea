package games.strategy.engine.pbem;

final class CredentialManagerException extends Exception {
  private static final long serialVersionUID = -110629801418732489L;

  CredentialManagerException(final String message) {
    super(message);
  }

  CredentialManagerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
