package games.strategy.engine.lobby;

import com.google.common.annotations.VisibleForTesting;
import lombok.NoArgsConstructor;
import org.triplea.lobby.common.LobbyConstants;

/** Utility class to verify player names. */
@NoArgsConstructor
public final class PlayerNameValidation {
  @VisibleForTesting static final int MAX_LENGTH = 40;
  private static final int MIN_LENGTH = 3;

  public static boolean isValid(final String username) {
    return validate(username) == null;
  }

  /**
   * Checks if a username is syntactical valid.
   *
   * @return Error message if username is not valid, otherwise returns an error message.
   */
  public static String validate(final String username) {
    return username != null
            && username.toLowerCase().contains(LobbyConstants.LOBBY_WATCHER_NAME.toLowerCase())
        ? LobbyConstants.LOBBY_WATCHER_NAME + " cannot be part of a name"
        : serverSideValidate(username);
  }

  /** Less restrictive validation intended to be done on server side. */
  public static String serverSideValidate(final String username) {
    if ((username == null) || (username.length() < MIN_LENGTH)) {
      return "Name is too short (minimum " + MIN_LENGTH + " characters)";
    } else if (username.length() > MAX_LENGTH) {
      return "Name is too long (maximum " + MAX_LENGTH + " characters)";
    } else if (!username.matches("[a-zA-Z][0-9a-zA-Z_-]+")) {
      return "Name can only contain alphanumeric characters, hyphens (-), and underscores (_)";
    } else if (username.toLowerCase().contains(LobbyConstants.ADMIN_USERNAME.toLowerCase())) {
      return "Name can't contain the word " + LobbyConstants.ADMIN_USERNAME;
    }
    return null;
  }
}
