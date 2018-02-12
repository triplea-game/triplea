package games.strategy.engine.lobby.server.login;

/**
 * The methods a user may use to access the lobby.
 */
public enum AccessMethod {
  /** The user must access the lobby with a valid set of credentials for a previously-registered account. */
  AUTHENTICATION,

  /** The user may access the lobby as a guest without having previously registered. */
  GUEST;
}
