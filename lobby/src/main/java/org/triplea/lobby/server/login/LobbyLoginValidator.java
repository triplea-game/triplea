package org.triplea.lobby.server.login;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.lobby.PlayerEmailValidation;
import games.strategy.engine.lobby.PlayerNameValidation;
import games.strategy.net.ILoginValidator;
import games.strategy.net.MacFinder;
import games.strategy.net.nio.ServerQuarantineConversation;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.DatabaseDao;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.dao.TempPasswordDao;
import org.triplea.lobby.server.login.forgot.password.verify.TempPasswordVerification;
import org.triplea.util.Version;

/**
 * The server side of the lobby authentication protocol.
 *
 * <p>In the lobby authentication protocol, the server sends a challenge to the client based on the
 * username of the client. Upon receiving the client's response, the server determines if the client
 * has provided a valid password and gives them access to the lobby if authentication is successful.
 */
@Log
@AllArgsConstructor
public final class LobbyLoginValidator implements ILoginValidator {
  @VisibleForTesting
  interface ErrorMessages {
    String ANONYMOUS_AUTHENTICATION_FAILED = "Can't login anonymously, username already exists";
    String AUTHENTICATION_FAILED = "Incorrect username or password";
    String INVALID_MAC = "Invalid mac address";
    String THAT_IS_NOT_A_NICE_NAME = "That's not a nice name";
    String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
    String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
    String TOO_MANY_FAILED_LOGIN_ATTEMPTS =
        "Too many failed login attempts, wait a few minutes before attempting again.";
  }

  private final DatabaseDao database;
  private final RsaAuthenticator rsaAuthenticator;
  private final Supplier<String> bcryptSaltGenerator;
  private final FailedLoginThrottle failedLoginThrottle;
  private final TempPasswordVerification tempPasswordVerification;
  private final TempPasswordDao tempPasswordDao;

  @Override
  public Map<String, String> getChallengeProperties(final String username) {
    return new HashMap<>(rsaAuthenticator.newChallenge());
  }

  @Nullable
  @Override
  public String verifyConnection(
      final Map<String, String> challenge,
      final Map<String, String> response,
      final String clientName,
      final String clientMac,
      final SocketAddress remoteAddress) {

    final var address = ((InetSocketAddress) remoteAddress).getAddress();
    if (failedLoginThrottle.tooManyFailedLoginAttempts(address)) {
      return ErrorMessages.TOO_MANY_FAILED_LOGIN_ATTEMPTS;
    }

    final User user =
        User.builder()
            .username(clientName)
            .inetAddress(((InetSocketAddress) remoteAddress).getAddress())
            .hashedMacAddress(clientMac)
            .build();
    final @Nullable String errorMessage = authenticateUser(response, user);
    if (errorMessage == null) {
      database.getAccessLogDao().insert(user, getUserTypeFor(response));
      return null;
    }
    if (errorMessage.equals(ServerQuarantineConversation.CHANGE_PASSWORD)) {
      database.getAccessLogDao().insert(user, getUserTypeFor(response));
      return errorMessage;
    }
    failedLoginThrottle.increment(address);
    return errorMessage;
  }

  private @Nullable String authenticateUser(final Map<String, String> response, final User user) {
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
    if (!MacFinder.isValidHashedMacAddress(user.getHashedMacAddress())) {
      // Must have been tampered with
      return ErrorMessages.INVALID_MAC;
    }
    final Optional<Timestamp> banExpiry =
        database.getBannedMacDao().isMacBanned(user.getInetAddress(), user.getHashedMacAddress());

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
    return response.containsKey(LobbyLoginResponseKeys.ANONYMOUS_LOGIN)
        ? UserType.ANONYMOUS
        : UserType.REGISTERED;
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

  private @Nullable String authenticateRegisteredUser(
      final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    final HashedPassword hashedPassword = database.getUserDao().getPassword(username);
    if (hashedPassword == null) {
      return ErrorMessages.AUTHENTICATION_FAILED;
    }

    if (RsaAuthenticator.canProcessResponse(response)) {
      return rsaAuthenticator.decryptPasswordForAction(
          response,
          pass -> {
            if (hashedPassword.isBcrypted()) {
              // TODO: update tests to verify tempPasswordVerification branch.
              if (tempPasswordVerification.checkTempPassword(username, pass)) {
                return ServerQuarantineConversation.CHANGE_PASSWORD;
              }

              return database.getUserDao().login(username, new HashedPassword(pass))
                  ? null
                  : ErrorMessages.AUTHENTICATION_FAILED;
            } else {
              return "Badly hashed password in client request";
            }
          });
    } else {
      return "Badly formatted client request";
    }
  }

  private @Nullable String authenticateAnonymousUser(
      final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    if (database.getUserDao().doesUserExist(username)) {
      return ErrorMessages.ANONYMOUS_AUTHENTICATION_FAILED;
    }
    // If this is a lobby watcher, use a different set of validation
    if (Boolean.TRUE.toString().equals(response.get(LobbyLoginResponseKeys.LOBBY_WATCHER_LOGIN))) {
      if (!username.endsWith(LobbyConstants.LOBBY_WATCHER_NAME)) {
        return "Lobby watcher usernames must end with 'lobby_watcher'";
      }
      final String hostName =
          username.substring(0, username.indexOf(LobbyConstants.LOBBY_WATCHER_NAME));
      return PlayerNameValidation.serverSideValidate(hostName);
    } else {
      return PlayerNameValidation.serverSideValidate(username);
    }
  }

  private @Nullable String createUser(final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    final String email = response.get(LobbyLoginResponseKeys.EMAIL);

    final String validationMessage =
        Optional.ofNullable(PlayerNameValidation.validate(username))
            .orElseGet(() -> PlayerEmailValidation.validate(email));
    if (validationMessage != null) {
      return validationMessage;
    }

    if (email.trim().equals("")) {
      return "Empty email address";
    }

    if (email.trim().contains(" ")) {
      return "Email address contains spaces";
    }

    if (database.getUserDao().doesUserExist(username)) {
      return "That user name has already been taken";
    }

    if (RsaAuthenticator.canProcessResponse(response)) {
      return rsaAuthenticator.decryptPasswordForAction(
          response,
          pass -> {
            final HashedPassword newPass =
                new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.get()));
            database.getUserDao().createUser(username, email, newPass);
            return null;
          });
    } else {
      return "Invalid client request";
    }
  }
}
