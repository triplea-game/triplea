package games.strategy.engine.lobby.server.login;

/**
 * The type of authentication used to grant access to the lobby.
 */
public enum AuthenticationType {
  /** The user authenticates anonymously as a guest without having registered. */
  ANONYMOUS,

  /** The user authenticates using a valid set of credentials for a registered account. */
  REGISTERED;
}
