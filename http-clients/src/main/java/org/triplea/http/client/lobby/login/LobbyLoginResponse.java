package org.triplea.http.client.lobby.login;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Represents data that would be uploaded to a server. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class LobbyLoginResponse {
  private final String loginToken;
  private final String failReason;

  /**
   * Factory method for creating login responses where login was not successful.
   *
   * @param reason A reason for why login failed, to be shown to user.
   */
  public static LobbyLoginResponse newFailResponse(final String reason) {
    return new LobbyLoginResponse(null, reason);
  }

  /**
   * Factory method for creating login response where login was successful.
   *
   * @param token Single-use token that can be used to establish a socket connection.
   */
  public static LobbyLoginResponse newSuccessResponse(final String token) {
    return new LobbyLoginResponse(token, null);
  }

  /** If present, indicates login was success. */
  public Optional<String> getLoginToken() {
    return Optional.ofNullable(loginToken);
  }

  /**
   * If login fails, this will return a 'reason' string that can be shown to the user. This is here
   * so we can tell a user that they were banned or if they simply got the wrong password.
   */
  public Optional<String> getFailReason() {
    return Optional.ofNullable(failReason);
  }
}
