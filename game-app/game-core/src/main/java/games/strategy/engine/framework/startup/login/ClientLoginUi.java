package games.strategy.engine.framework.startup.login;

/** User interaction needed by {@link ClientLogin}. */
public interface ClientLoginUi {
  /** Asks the user for the game password; returns an empty string if the user enters none. */
  String promptForPassword();

  /**
   * Tells the user the host logged them in under {@code assignedName}, an existing login, rather
   * than the name they asked for.
   */
  void showAlreadyLoggedIn(String assignedName);
}
