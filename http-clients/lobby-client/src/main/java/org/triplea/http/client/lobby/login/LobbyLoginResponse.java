package org.triplea.http.client.lobby.login;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Represents data that would be uploaded to a server. */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class LobbyLoginResponse {
  /** On successful login, a non-null API key token is passed back from the server. */
  @Nullable private final String apiKey;
  /** When non-null, indicates login did not occur, reason for failed login will be present. */
  @Nullable private final String failReason;
  /** True if the authenticated user is a moderator. */
  private final boolean moderator;
  /**
   * True indicates user has used a temporary password that is now expired and should change their
   * password.
   */
  private final boolean passwordChangeRequired;
  /**
   * When users log in to lobby successfully, we optionally can optionally send them a banner
   * message that is printed on the lobby screen.
   */
  private final String lobbyMessage;

  public boolean isSuccess() {
    return apiKey != null;
  }

  public String getFailReason() {
    Preconditions.checkState(failReason == null ^ apiKey == null);
    return failReason;
  }

  public String getApiKey() {
    Preconditions.checkState(failReason == null ^ apiKey == null);
    return apiKey;
  }
}
