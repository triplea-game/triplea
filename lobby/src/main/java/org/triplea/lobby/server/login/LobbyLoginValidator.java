package org.triplea.lobby.server.login;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginChallengeKeys;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.DatabaseDao;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.util.Md5Crypt;
import org.triplea.util.Tuple;
import org.triplea.util.Version;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.ILoginValidator;
import games.strategy.net.MacFinder;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/**
 * The server side of the lobby authentication protocol.
 *
 * <p>
 * In the lobby authentication protocol, the server sends a challenge to the client based on the username of the client.
 * Upon receiving the client's response, the server determines if the client has provided a valid password and gives
 * them access to the lobby if authentication is successful.
 * </p>
 */
@Log
@AllArgsConstructor
public final class LobbyLoginValidator implements ILoginValidator {
  @VisibleForTesting
  interface ErrorMessages {
    String ANONYMOUS_AUTHENTICATION_FAILED = "Can't login anonymously, username already exists";
    String AUTHENTICATION_FAILED = "Incorrect username or password";
    String INVALID_MAC = "Invalid mac address";
    String THATS_NOT_A_NICE_NAME = "That's not a nice name";
    String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
    String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
  }

  private final DatabaseDao database;
  private final RsaAuthenticator rsaAuthenticator;
  private final Supplier<String> bcryptSaltGenerator;


  @Override
  public Map<String, String> getChallengeProperties(final String userName) {
    final Map<String, String> challenge = new HashMap<>();
    challenge.putAll(newMd5CryptAuthenticatorChallenge(userName));
    challenge.putAll(rsaAuthenticator.newChallenge());
    return challenge;
  }

  private Map<String, String> newMd5CryptAuthenticatorChallenge(final String userName) {
    final Map<String, String> challenge = new HashMap<>();
    final HashedPassword password = database.getUserDao().getLegacyPassword(userName);
    if (password != null && Strings.emptyToNull(password.value) != null) {
      challenge.put(LobbyLoginChallengeKeys.SALT, Md5Crypt.getSalt(password.value));
    }
    return challenge;
  }

  @Nullable
  @Override
  public String verifyConnection(
      final Map<String, String> challenge,
      final Map<String, String> response,
      final String clientName,
      final String clientMac,
      final SocketAddress remoteAddress) {
    final User user = User.builder()
        .username(clientName)
        .inetAddress(((InetSocketAddress) remoteAddress).getAddress())
        .hashedMacAddress(clientMac)
        .build();
    final @Nullable String errorMessage = authenticateUser(response, user);
    logAuthenticationResult(user, getUserTypeFor(response), errorMessage);
    return errorMessage;
  }

  private @Nullable String authenticateUser(final Map<String, String> response, final User user) {
    if (response == null) {
      return "No Client Properties";
    }
    final String clientVersionString = response.get(LobbyLoginResponseKeys.LOBBY_VERSION);
    if (clientVersionString == null) {
      return "No Client Version";
    }
    final Version clientVersion = new Version(clientVersionString);
    if (!clientVersion.equals(LobbyConstants.LOBBY_VERSION)) {
      return "Wrong version, we require " + LobbyConstants.LOBBY_VERSION.toString() + " but trying to log in with "
          + clientVersionString;
    }
    for (final String s : getBadWords()) {
      if (user.getUsername().toLowerCase().contains(s.toLowerCase())) {
        return ErrorMessages.THATS_NOT_A_NICE_NAME;
      }
    }
    if (!MacFinder.isValidHashedMacAddress(user.getHashedMacAddress())) {
      // Must have been tampered with
      return ErrorMessages.INVALID_MAC;
    }
    final Tuple<Boolean, Timestamp> macBanned = database.getBannedMacDao().isMacBanned(user.getHashedMacAddress());
    if (macBanned.getFirst()) {
      return ErrorMessages.YOU_HAVE_BEEN_BANNED + " " + getBanDurationBreakdown(macBanned.getSecond());
    }
    // test for username ban after testing normal bans, because if it is only a username ban then the user should know
    // they can change their name
    final Tuple<Boolean, Timestamp> usernameBanned =
        database.getBannedUsernameDao().isUsernameBanned(user.getUsername());
    if (usernameBanned.getFirst()) {
      return ErrorMessages.USERNAME_HAS_BEEN_BANNED + " " + getBanDurationBreakdown(usernameBanned.getSecond());
    }
    if (response.containsKey(LobbyLoginResponseKeys.REGISTER_NEW_USER)) {
      return createUser(response, user);
    }

    final UserType userType = getUserTypeFor(response);
    switch (userType) {
      case ANONYMOUS:
        return authenticateAnonymousUser(response, user);
      case REGISTERED:
        return authenticateRegisteredUser(response, user);
      default:
        throw new AssertionError("unknown user type: " + userType);
    }
  }

  private static UserType getUserTypeFor(final Map<String, String> response) {
    return response.containsKey(LobbyLoginResponseKeys.ANONYMOUS_LOGIN) ? UserType.ANONYMOUS : UserType.REGISTERED;
  }

  private void logAuthenticationResult(final User user, final UserType userType, final @Nullable String errorMessage) {
    if (errorMessage == null) {
      try {
        database.getAccessLogDao().insert(user, userType);
      } catch (final SQLException e) {
        log.log(Level.SEVERE, "failed to record successful authentication in database", e);
      }
    }
  }

  private static String getBanDurationBreakdown(final Timestamp stamp) {
    if (stamp == null) {
      return "Banned Forever";
    }
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

  private List<String> getBadWords() {
    return database.getBadWordDao().list();
  }

  private @Nullable String authenticateRegisteredUser(final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    final String errorMessage = ErrorMessages.AUTHENTICATION_FAILED;
    final HashedPassword hashedPassword = database.getUserDao().getPassword(username);
    if (hashedPassword == null) {
      return errorMessage;
    }
    if (RsaAuthenticator.canProcessResponse(response)) {
      return rsaAuthenticator.decryptPasswordForAction(response, pass -> {
        final String legacyHashedPassword = response.get(LobbyLoginResponseKeys.HASHED_PASSWORD);
        if (hashedPassword.isBcrypted()) {
          if (database.getUserDao().login(username, new HashedPassword(pass))) {
            if (legacyHashedPassword != null && database.getUserDao().getLegacyPassword(username).value.isEmpty()) {
              database.getUserDao().updateUser(database.getUserDao().getUserByName(username),
                  new HashedPassword(legacyHashedPassword));
              database.getUserDao().updateUser(database.getUserDao().getUserByName(username),
                  new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.get())));
            }
            return null;
          }
          return errorMessage;
        } else if (database.getUserDao().login(username, new HashedPassword(legacyHashedPassword))) {
          database.getUserDao().updateUser(database.getUserDao().getUserByName(username),
              new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.get())));
          return null;
        } else {
          return errorMessage;
        }
      });
    }
    if (!database.getUserDao().login(username,
        new HashedPassword(response.get(LobbyLoginResponseKeys.HASHED_PASSWORD)))) {
      return errorMessage;
    }
    return null;
  }

  private @Nullable String authenticateAnonymousUser(final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    if (database.getUserDao().doesUserExist(username)) {
      return ErrorMessages.ANONYMOUS_AUTHENTICATION_FAILED;
    }
    // If this is a lobby watcher, use a different set of validation
    if (Boolean.TRUE.toString().equals(response.get(LobbyLoginResponseKeys.LOBBY_WATCHER_LOGIN))) {
      if (!username.endsWith(LobbyConstants.LOBBY_WATCHER_NAME)) {
        return "Lobby watcher usernames must end with 'lobby_watcher'";
      }
      final String hostName = username.substring(0, username.indexOf(LobbyConstants.LOBBY_WATCHER_NAME));

      if (!DBUser.isValidUserName(hostName)) {
        return DBUser.getUserNameValidationErrorMessage(hostName);
      }
    } else {
      return DBUser.isValidUserName(username) ? null : DBUser.getUserNameValidationErrorMessage(username);
    }
    return null;
  }

  private @Nullable String createUser(final Map<String, String> response, final User user) {
    final DBUser dbUser = new DBUser(
        new DBUser.UserName(user.getUsername()),
        new DBUser.UserEmail(response.get(LobbyLoginResponseKeys.EMAIL)));

    if (!dbUser.isValid()) {
      return dbUser.getValidationErrorMessage();
    }

    if (database.getUserDao().doesUserExist(dbUser.getName())) {
      return "That user name has already been taken";
    }

    final HashedPassword password = new HashedPassword(response.get(LobbyLoginResponseKeys.HASHED_PASSWORD));
    if (RsaAuthenticator.canProcessResponse(response)) {
      return rsaAuthenticator.decryptPasswordForAction(response, pass -> {
        final HashedPassword newPass = new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.get()));
        if (password.isHashedWithSalt()) {
          database.getUserDao().createUser(dbUser, password);
          database.getUserDao().updateUser(dbUser, newPass);
        } else {
          database.getUserDao().createUser(dbUser, newPass);
        }
        return null;
      });
    }
    if (!password.isHashedWithSalt()) {
      return "Password is not hashed correctly";
    }

    try {
      database.getUserDao().createUser(dbUser, password);
      return null;
    } catch (final Exception e) {
      return e.getMessage();
    }
  }
}
