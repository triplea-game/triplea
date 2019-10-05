package org.triplea.lobby.server.login;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.engine.lobby.PlayerNameValidation;
import games.strategy.net.ILoginValidator;
import games.strategy.net.nio.ServerQuarantineConversation;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
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
    String TOO_MANY_FAILED_LOGIN_ATTEMPTS =
        "Too many failed login attempts, wait a few minutes before attempting again.";
  }

  private final DatabaseDao database;
  private final RsaAuthenticator rsaAuthenticator;
  private final Supplier<String> bcryptSaltGenerator;
  private final FailedLoginThrottle failedLoginThrottle;
  private final BiPredicate<PlayerName, String> tempPasswordVerification;
  private final AllowLoginRules allowLoginRules;
  private final AllowCreateUserRules allowCreateUserRules;

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
      return LobbyLoginValidator.ErrorMessages.TOO_MANY_FAILED_LOGIN_ATTEMPTS;
    }

    final User user =
        User.builder()
            .username(clientName)
            .inetAddress(((InetSocketAddress) remoteAddress).getAddress())
            .hashedMacAddress(clientMac)
            .build();
    final @Nullable String errorMessage =
        Optional.ofNullable(allowLoginRules.checkLoginIsAllowed(response, user))
            .orElseGet(() -> authenticateUser(response, user));

    if (errorMessage == null || errorMessage.equals(ServerQuarantineConversation.CHANGE_PASSWORD)) {
      database.getAccessLogDao().insert(user, getUserTypeFor(response));
      return errorMessage;
    }
    failedLoginThrottle.increment(address);
    return errorMessage;
  }

  private static UserType getUserTypeFor(final Map<String, String> response) {
    return response.containsKey(LobbyLoginResponseKeys.ANONYMOUS_LOGIN)
        ? UserType.ANONYMOUS
        : UserType.REGISTERED;
  }

  private @Nullable String authenticateUser(final Map<String, String> response, final User user) {
    if (response.containsKey(LobbyLoginResponseKeys.ANONYMOUS_LOGIN)) {
      return authenticateAnonymousUser(response, user);
    } else if (response.containsKey(LobbyLoginResponseKeys.REGISTER_NEW_USER)) {
      return createUser(response, user);
    }
    return authenticateRegisteredUser(response, user);
  }

  private @Nullable String authenticateRegisteredUser(
      final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    final HashedPassword hashedPassword = database.getUserDao().getPassword(username);
    if (hashedPassword == null) {
      return ErrorMessages.AUTHENTICATION_FAILED;
    }

    return rsaAuthenticator.decryptPasswordForAction(
        response,
        pass -> {
          if (hashedPassword.isBcrypted()) {
            // TODO: update tests to verify tempPasswordVerification branch.
            if (tempPasswordVerification.test(PlayerName.of(username), pass)) {
              return ServerQuarantineConversation.CHANGE_PASSWORD;
            }

            return database.getUserDao().login(username, pass)
                ? null
                : ErrorMessages.AUTHENTICATION_FAILED;
          } else {
            return "Badly hashed password in client request";
          }
        });
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
    final String errMsg =
        allowCreateUserRules.allowCreateUser(
            user.getUsername(), response.get(LobbyLoginResponseKeys.EMAIL));
    if (errMsg != null) {
      return errMsg;
    }

    final String username = user.getUsername();
    final String email = response.get(LobbyLoginResponseKeys.EMAIL);

    return rsaAuthenticator.decryptPasswordForAction(
        response,
        pass -> {
          final HashedPassword newPass =
              new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.get()));
          database.getUserDao().createUser(username, email, newPass);
          return null;
        });
  }
}
