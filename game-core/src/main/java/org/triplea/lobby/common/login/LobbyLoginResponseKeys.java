package org.triplea.lobby.common.login;

/** The property keys that may be present in a lobby authentication protocol response. */
public final class LobbyLoginResponseKeys {
  public static final String ANONYMOUS_LOGIN = "ANONYMOUS_LOGIN";
  public static final String EMAIL = "EMAIL";
  public static final String HASHED_PASSWORD = "HASHEDPWD";
  public static final String LOBBY_VERSION = "LOBBY_VERSION";
  public static final String LOBBY_WATCHER_LOGIN = "LOBBY_WATCHER_LOGIN";
  public static final String REGISTER_NEW_USER = "REGISTER_USER";
  public static final String RSA_ENCRYPTED_PASSWORD = "RSAPWD";

  private LobbyLoginResponseKeys() {}
}
