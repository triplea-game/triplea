package games.strategy.engine.framework.startup.login;

final class AuthenticationException extends Exception {
  private static final long serialVersionUID = 1919583473465360072L;

  AuthenticationException(final String message) {
    super(message);
  }

  AuthenticationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
