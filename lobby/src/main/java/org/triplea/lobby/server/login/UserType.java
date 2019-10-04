package org.triplea.lobby.server.login;

/** The type of a lobby user. */
public enum UserType {
  /**
   * A user who has not registered. Anonymous users are guests who do not need to provide
   * credentials in order to be granted access to the lobby.
   */
  ANONYMOUS,

  /**
   * A user who has registered. Registered users must provide valid credentials in order to be
   * granted access to the lobby.
   */
  REGISTERED
}
