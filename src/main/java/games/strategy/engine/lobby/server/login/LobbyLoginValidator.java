package games.strategy.engine.lobby.server.login;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.lobby.server.LobbyContext;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.db.BadWordController;
import games.strategy.engine.lobby.server.db.BadWordDao;
import games.strategy.engine.lobby.server.db.BannedMacController;
import games.strategy.engine.lobby.server.db.BannedMacDao;
import games.strategy.engine.lobby.server.db.BannedUsernameController;
import games.strategy.engine.lobby.server.db.BannedUsernameDao;
import games.strategy.engine.lobby.server.db.HashedPassword;
import games.strategy.engine.lobby.server.db.UserController;
import games.strategy.engine.lobby.server.db.UserDao;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.ILoginValidator;
import games.strategy.util.Tuple;
import games.strategy.util.Version;

/**
 * The server side of the lobby authentication protocol.
 *
 * <p>
 * In the lobby authentication protocol, the server sends a challenge to the client based on the username of the client.
 * Upon receiving the client's response, the server determines if the client has provided a valid password and gives
 * them access to the lobby if authentication is successful.
 * </p>
 */
public final class LobbyLoginValidator implements ILoginValidator {
  public static final String LOBBY_VERSION = "LOBBY_VERSION";
  public static final String REGISTER_NEW_USER_KEY = "REGISTER_USER";
  public static final String ANONYMOUS_LOGIN = "ANONYMOUS_LOGIN";
  public static final String LOBBY_WATCHER_LOGIN = "LOBBY_WATCHER_LOGIN";
  public static final String HASHED_PASSWORD_KEY = "HASHEDPWD";
  public static final String EMAIL_KEY = "EMAIL";
  public static final String SALT_KEY = "SALT";

  @VisibleForTesting
  interface ErrorMessages {
    String AUTHENTICATION_FAILED = "Incorrect username or password";
    String INVALID_MAC = "Invalid mac address";
    String MAINTENANCE_MODE_ENABLED = "The lobby is in maintenance mode; please try again later";
    String THATS_NOT_A_NICE_NAME = "That's not a nice name";
    String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address";
    String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
    String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
  }

  private static final Logger logger = Logger.getLogger(LobbyLoginValidator.class.getName());

  private final BadWordDao badWordDao;
  private final BannedMacDao bannedMacDao;
  private final BannedUsernameDao bannedUsernameDao;
  private final BcryptSaltGenerator bcryptSaltGenerator;
  private final LobbyPropertyReader lobbyPropertyReader;
  private final RsaAuthenticator rsaAuthenticator;
  private final UserDao userDao;

  public LobbyLoginValidator() {
    this(
        LobbyContext.lobbyPropertyReader(),
        new BadWordController(),
        new BannedMacController(),
        new BannedUsernameController(),
        new UserController(),
        new RsaAuthenticator(),
        () -> BCrypt.gensalt());
  }

  @VisibleForTesting
  LobbyLoginValidator(
      final LobbyPropertyReader lobbyPropertyReader,
      final BadWordDao badWordDao,
      final BannedMacDao bannedMacDao,
      final BannedUsernameDao bannedUsernameDao,
      final UserDao userDao,
      final RsaAuthenticator rsaAuthenticator,
      final BcryptSaltGenerator bcryptSaltGenerator) {
    this.badWordDao = badWordDao;
    this.bannedMacDao = bannedMacDao;
    this.bannedUsernameDao = bannedUsernameDao;
    this.bcryptSaltGenerator = bcryptSaltGenerator;
    this.lobbyPropertyReader = lobbyPropertyReader;
    this.rsaAuthenticator = rsaAuthenticator;
    this.userDao = userDao;
  }

  @Override
  public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress) {
    final Map<String, String> challenge = new HashMap<>();
    challenge.putAll(newMd5CryptAuthenticatorChallenge(userName));
    challenge.putAll(rsaAuthenticator.newChallenge());
    return challenge;
  }

  private Map<String, String> newMd5CryptAuthenticatorChallenge(final String userName) {
    final Map<String, String> challenge = new HashMap<>();
    if (lobbyPropertyReader.isMaintenanceMode()) {
      challenge.put(SALT_KEY, games.strategy.util.MD5Crypt.newSalt());
    } else {
      final HashedPassword password = userDao.getLegacyPassword(userName);
      if (password != null && Strings.emptyToNull(password.value) != null) {
        challenge.put(SALT_KEY, games.strategy.util.MD5Crypt.getSalt(password.value));
      }
    }
    return challenge;
  }

  @Override
  public String verifyConnection(final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient, final String clientName, final String clientMac,
      final SocketAddress remoteAddress) {
    if (lobbyPropertyReader.isMaintenanceMode()) {
      return ErrorMessages.MAINTENANCE_MODE_ENABLED;
    }

    final String error = verifyConnectionInternal(propertiesReadFromClient, clientName, clientMac);
    if (error != null) {
      logger.info("Bad login attempt from " + remoteAddress + " for user " + clientName + " error:" + error);
      AccessLog.failedLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress(), error);
    } else {
      logger.info("Successful login from:" + remoteAddress + " for user:" + clientName);
      AccessLog.successfulLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress());
    }
    return error;
  }

  private String verifyConnectionInternal(
      final Map<String, String> propertiesReadFromClient,
      final String clientName,
      final String hashedMac) {
    if (propertiesReadFromClient == null) {
      return "No Client Properties";
    }
    final String clientVersionString = propertiesReadFromClient.get(LOBBY_VERSION);
    if (clientVersionString == null) {
      return "No Client Version";
    }
    final Version clientVersion = new Version(clientVersionString);
    if (!clientVersion.equals(LobbyServer.LOBBY_VERSION)) {
      return "Wrong version, we require " + LobbyServer.LOBBY_VERSION.toString() + " but trying to log in with "
          + clientVersionString;
    }
    for (final String s : getBadWords()) {
      if (clientName.toLowerCase().contains(s.toLowerCase())) {
        return ErrorMessages.THATS_NOT_A_NICE_NAME;
      }
    }
    if (hashedMac == null) {
      return ErrorMessages.UNABLE_TO_OBTAIN_MAC;
    }
    if (hashedMac.length() != 28 || !hashedMac.startsWith(games.strategy.util.MD5Crypt.MAGIC + "MH$")
        || !hashedMac.matches("[0-9a-zA-Z$./]+")) {
      // Must have been tampered with
      return ErrorMessages.INVALID_MAC;
    }
    final Tuple<Boolean, Timestamp> macBanned = bannedMacDao.isMacBanned(hashedMac);
    if (macBanned.getFirst()) {
      return ErrorMessages.YOU_HAVE_BEEN_BANNED + " " + getBanDurationBreakdown(macBanned.getSecond());
    }
    // test for username ban after testing normal bans, because if it is only a username ban then the user should know
    // they can change their
    // name
    final Tuple<Boolean, Timestamp> usernameBanned = bannedUsernameDao.isUsernameBanned(clientName);
    if (usernameBanned.getFirst()) {
      return ErrorMessages.USERNAME_HAS_BEEN_BANNED + " " + getBanDurationBreakdown(usernameBanned.getSecond());
    }
    if (propertiesReadFromClient.containsKey(REGISTER_NEW_USER_KEY)) {
      return createUser(propertiesReadFromClient, clientName);
    }

    return propertiesReadFromClient.containsKey(ANONYMOUS_LOGIN)
        ? anonymousLogin(propertiesReadFromClient, clientName)
        : validatePassword(propertiesReadFromClient, clientName);
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
      sb.append(days);
      sb.append(" Days ");
    }
    if (hours > 0) {
      sb.append(hours);
      sb.append(" Hours ");
    }
    if (minutes > 0) {
      sb.append(minutes);
      sb.append(" Minutes ");
    }
    return (sb.toString());
  }

  private List<String> getBadWords() {
    return badWordDao.list();
  }

  private String validatePassword(final Map<String, String> propertiesReadFromClient, final String clientName) {
    final String errorMessage = ErrorMessages.AUTHENTICATION_FAILED;
    final HashedPassword hashedPassword = userDao.getPassword(clientName);
    if (hashedPassword == null) {
      return errorMessage;
    }
    if (RsaAuthenticator.canProcessResponse(propertiesReadFromClient)) {
      return rsaAuthenticator.decryptPasswordForAction(propertiesReadFromClient, pass -> {
        final String legacyHashedPassword = propertiesReadFromClient.get(HASHED_PASSWORD_KEY);
        if (hashedPassword.isBcrypted()) {
          if (userDao.login(clientName, new HashedPassword(pass))) {
            if (legacyHashedPassword != null && userDao.getLegacyPassword(clientName).value.isEmpty()) {
              userDao.updateUser(userDao.getUserByName(clientName), new HashedPassword(legacyHashedPassword));
              userDao.updateUser(userDao.getUserByName(clientName),
                  new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.newSalt())));
            }
            return null;
          }
          return errorMessage;
        } else if (userDao.login(clientName, new HashedPassword(legacyHashedPassword))) {
          userDao.updateUser(userDao.getUserByName(clientName),
              new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.newSalt())));
          return null;
        } else {
          return errorMessage;
        }
      });
    }
    if (!userDao.login(clientName, new HashedPassword(propertiesReadFromClient.get(HASHED_PASSWORD_KEY)))) {
      return errorMessage;
    }
    return null;
  }

  private String anonymousLogin(final Map<String, String> propertiesReadFromClient, final String userName) {
    if (userDao.doesUserExist(userName)) {
      return "Can't login anonymously, username already exists";
    }
    // If this is a lobby watcher, use a different set of validation
    if (propertiesReadFromClient.get(LOBBY_WATCHER_LOGIN) != null
        && propertiesReadFromClient.get(LOBBY_WATCHER_LOGIN).equals(Boolean.TRUE.toString())) {
      if (!userName.endsWith(InGameLobbyWatcher.LOBBY_WATCHER_NAME)) {
        return "Lobby watcher usernames must end with 'lobby_watcher'";
      }
      final String hostName = userName.substring(0, userName.indexOf(InGameLobbyWatcher.LOBBY_WATCHER_NAME));

      if (!DBUser.isValidUserName(hostName)) {
        return DBUser.getUserNameValidationErrorMessage(hostName);
      }
    } else {
      return DBUser.isValidUserName(userName) ? null : DBUser.getUserNameValidationErrorMessage(userName);
    }
    return null;
  }

  private String createUser(final Map<String, String> propertiesReadFromClient, final String userName) {
    final DBUser user = new DBUser(
        new DBUser.UserName(userName),
        new DBUser.UserEmail(propertiesReadFromClient.get(EMAIL_KEY)));

    if (!user.isValid()) {
      return user.getValidationErrorMessage();
    }

    if (userDao.doesUserExist(user.getName())) {
      return "That user name has already been taken";
    }

    final HashedPassword password = new HashedPassword(propertiesReadFromClient.get(HASHED_PASSWORD_KEY));
    if (RsaAuthenticator.canProcessResponse(propertiesReadFromClient)) {
      return rsaAuthenticator.decryptPasswordForAction(propertiesReadFromClient, pass -> {
        final HashedPassword newPass = new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.newSalt()));
        if (password.isHashedWithSalt()) {
          userDao.createUser(user, password);
          userDao.updateUser(user, newPass);
        } else {
          userDao.createUser(user, newPass);
        }
        return null;
      });
    }
    if (!password.isHashedWithSalt()) {
      return "Password is not hashed correctly";
    }

    try {
      userDao.createUser(user, password);
      return null;
    } catch (final Exception e) {
      return e.getMessage();
    }
  }

  @FunctionalInterface
  @VisibleForTesting
  interface BcryptSaltGenerator {
    String newSalt();
  }
}
