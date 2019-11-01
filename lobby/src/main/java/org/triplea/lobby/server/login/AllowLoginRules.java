package org.triplea.lobby.server.login;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.net.MacFinder;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.DatabaseDao;
import org.triplea.util.Version;

/** Detects if a given request should be allowed to login. */
@AllArgsConstructor
// TODO: unit test this class, then inject a mock of AllowLoginRules into LobbyLoginValidatorTest
// and simplify those tests.
class AllowLoginRules {

  private final DatabaseDao database;

  @VisibleForTesting
  interface ErrorMessages {
    String INVALID_MAC = "Invalid mac address";
    String THAT_IS_NOT_A_NICE_NAME = "That's not a nice name";
    String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
    String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
  }

  @Nullable
  String checkLoginIsAllowed(final Map<String, String> response, final User user) {

    final String clientVersionString = response.get(LobbyLoginResponseKeys.LOBBY_VERSION);
    if (clientVersionString == null) {
      return "No Client Version";
    }
    final Version clientVersion = new Version(clientVersionString);
    if (!clientVersion.equals(LobbyConstants.LOBBY_VERSION)) {
      return "Wrong version, we require "
          + LobbyConstants.LOBBY_VERSION.toString()
          + " but trying to log in with "
          + clientVersionString;
    }
    if (database.getBadWordDao().containsBadWord(user.getUsername())) {
      return ErrorMessages.THAT_IS_NOT_A_NICE_NAME;
    }
    if (!MacFinder.isValidHashedMacAddress(user.getSystemId())) {
      // Must have been tampered with
      return ErrorMessages.INVALID_MAC;
    }
    final Optional<Timestamp> banExpiry =
        database.getBannedMacDao().isBanned(user.getInetAddress(), user.getSystemId());

    if (banExpiry.isPresent() && banExpiry.get().toInstant().isAfter(Instant.now())) {
      return ErrorMessages.YOU_HAVE_BEEN_BANNED + " " + getBanDurationBreakdown(banExpiry.get());
    }
    // test for username ban after testing normal bans, because if it is only a username ban then
    // the user should know
    // they can change their name
    final boolean usernameBanned =
        database.getUsernameBlacklistDao().isUsernameBanned(user.getUsername());
    if (usernameBanned) {
      return ErrorMessages.USERNAME_HAS_BEEN_BANNED;
    }

    if (!response.containsKey(LobbyLoginResponseKeys.ANONYMOUS_LOGIN)
        && !response.containsKey(LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD)) {
      return "Invalid client request";
    }

    return null;
  }

  private static String getBanDurationBreakdown(final Timestamp stamp) {
    final long millis = stamp.getTime() - System.currentTimeMillis();
    if (millis < 0) {
      return "Ban time left: 1 Minute";
    }
    long seconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(millis));
    final int minutesInSeconds = 60;
    final int hoursInSeconds = 60 * 60;
    final int daysInSeconds = 60 * 60 * 24;
    final long days = seconds / daysInSeconds;
    seconds -= days * daysInSeconds;
    final long hours = seconds / hoursInSeconds;
    seconds -= hours * hoursInSeconds;
    final long minutes = Math.max(1, seconds / minutesInSeconds);

    final StringBuilder sb = new StringBuilder(64);
    sb.append("Ban time left: ");
    if (days > 0) {
      sb.append(days).append(" Days ");
    }
    if (hours > 0) {
      sb.append(hours).append(" Hours ");
    }
    sb.append(minutes).append(" Minutes");
    return sb.toString();
  }
}
