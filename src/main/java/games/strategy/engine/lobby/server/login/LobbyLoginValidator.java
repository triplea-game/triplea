package games.strategy.engine.lobby.server.login;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Strings;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.userDB.BadWordController;
import games.strategy.engine.lobby.server.userDB.BannedMacController;
import games.strategy.engine.lobby.server.userDB.BannedUsernameController;
import games.strategy.engine.lobby.server.userDB.DbUser;
import games.strategy.engine.lobby.server.userDB.DbUserController;
import games.strategy.engine.lobby.server.userDB.HashedPassword;
import games.strategy.net.ILoginValidator;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Tuple;
import games.strategy.util.Version;

public class LobbyLoginValidator implements ILoginValidator {
  static final String THATS_NOT_A_NICE_NAME = "That's not a nice name.";
  static final String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
  static final String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
  static final String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address.";
  static final String INVALID_MAC = "Invalid mac address.";
  private static final Logger s_logger = Logger.getLogger(LobbyLoginValidator.class.getName());
  public static final String LOBBY_VERSION = "LOBBY_VERSION";
  public static final String REGISTER_NEW_USER_KEY = "REGISTER_USER";
  public static final String ANONYMOUS_LOGIN = "ANONYMOUS_LOGIN";
  public static final String LOBBY_WATCHER_LOGIN = "LOBBY_WATCHER_LOGIN";
  public static final String HASHED_PASSWORD_KEY = "HASHEDPWD";
  public static final String EMAIL_KEY = "EMAIL";
  public static final String SALT_KEY = "SALT";

  public LobbyLoginValidator() {}

  @Override
  public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress) {
    // we need to give the user the salt key for the username
    final String password = new DbUserController().getPassword(userName).value;
    final Map<String, String> rVal = new HashMap<>();
    if (!Strings.nullToEmpty(password).isEmpty()) {
      rVal.put(SALT_KEY, MD5Crypt.getSalt(MD5Crypt.MAGIC, password));
    }
    return rVal;
  }

  @Override
  public String verifyConnection(final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient, final String clientName, final String clientMac,
      final SocketAddress remoteAddress) {
    final String error = verifyConnectionInternal(propertiesReadFromClient, clientName, clientMac);
    if (error != null) {
      s_logger.info("Bad login attempt from " + remoteAddress + " for user " + clientName + " error:" + error);
      AccessLog.failedLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress(), error);
    } else {
      s_logger.info("Successful login from:" + remoteAddress + " for user:" + clientName);
      AccessLog.successfulLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress());
    }
    return error;
  }

  private static String verifyConnectionInternal(final Map<String, String> propertiesReadFromClient,
      final String clientName, final String hashedMac) {
    if (propertiesReadFromClient == null) {
      return "No Client Properties";
    }
    final String clientVersionString = propertiesReadFromClient.get(LOBBY_VERSION);
    if (clientVersionString == null) {
      return "No Client Version";
    }
    final Version clientVersion = new Version(clientVersionString);
    if (!clientVersion.equals(LobbyServer.LOBBY_VERSION)) {
      return "Wrong version, we require" + LobbyServer.LOBBY_VERSION.toString() + " but trying to log in with "
          + clientVersionString;
    }
    for (final String s : getBadWords()) {
      if (clientName.toLowerCase().contains(s.toLowerCase())) {
        return THATS_NOT_A_NICE_NAME;
      }
    }
    if (hashedMac == null) {
      return UNABLE_TO_OBTAIN_MAC;
    }
    if (hashedMac.length() != 28 || !hashedMac.startsWith(MD5Crypt.MAGIC + "MH$")
        || !hashedMac.matches("[0-9a-zA-Z$./]+")) {
      // Must have been tampered with
      return INVALID_MAC;
    }
    final Tuple<Boolean, Timestamp> macBanned = new BannedMacController().isMacBanned(hashedMac);
    if (macBanned.getFirst()) {
      return YOU_HAVE_BEEN_BANNED + " " + getBanDurationBreakdown(macBanned.getSecond());
    }
    // test for username ban after testing normal bans, because if it is only a username ban then the user should know
    // they can change their
    // name
    final Tuple<Boolean, Timestamp> usernameBanned = new BannedUsernameController().isUsernameBanned(clientName);
    if (usernameBanned.getFirst()) {
      return USERNAME_HAS_BEEN_BANNED + " " + getBanDurationBreakdown(usernameBanned.getSecond());
    }
    if (propertiesReadFromClient.containsKey(REGISTER_NEW_USER_KEY)) {
      return createUser(propertiesReadFromClient, clientName);
    }
    if (propertiesReadFromClient.containsKey(ANONYMOUS_LOGIN)) {
      return anonymousLogin(propertiesReadFromClient, clientName);
    } else {
      return validatePassword(propertiesReadFromClient, clientName);
    }
  }

  static String getBanDurationBreakdown(final Timestamp stamp) {
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

  private static List<String> getBadWords() {
    return new BadWordController().list();
  }

  private static String validatePassword(final Map<String, String> propertiesReadFromClient, final String clientName) {
    final DbUserController dbUserController = new DbUserController();
    if (!dbUserController.login(clientName, new HashedPassword(propertiesReadFromClient.get(HASHED_PASSWORD_KEY)))) {
      if (dbUserController.doesUserExist(clientName)) {
        return "Incorrect password";
      } else {
        return "Username does not exist";
      }
    } else {
      return null;
    }
  }

  private static String anonymousLogin(final Map<String, String> propertiesReadFromClient, final String userName) {
    if (new DbUserController().doesUserExist(userName)) {
      return "Can't login anonymously, username already exists";
    }
    // If this is a lobby watcher, use a different set of validation
    if (propertiesReadFromClient.get(LOBBY_WATCHER_LOGIN) != null
        && propertiesReadFromClient.get(LOBBY_WATCHER_LOGIN).equals(Boolean.TRUE.toString())) {
      if (!userName.endsWith(InGameLobbyWatcher.LOBBY_WATCHER_NAME)) {
        return "Lobby watcher usernames must end with 'lobby_watcher'";
      }
      final String hostName = userName.substring(0, userName.indexOf(InGameLobbyWatcher.LOBBY_WATCHER_NAME));

      if (!DbUser.isValidUserName(hostName)) {
        return DbUser.getUserNameValidationErrorMessage(hostName);
      }
    } else {
      if (DbUser.isValidUserName(userName)) {
        return null;
      } else {
        return DbUser.getUserNameValidationErrorMessage(userName);
      }
    }
    return null;
  }

  private static String createUser(final Map<String, String> propertiesReadFromClient, final String userName) {
    final DbUser user = new DbUser(
        new DbUser.UserName(userName),
        new DbUser.UserEmail(propertiesReadFromClient.get(EMAIL_KEY)));

    if (!user.isValid()) {
      return user.getValidationErrorMessage();
    }

    final HashedPassword password = new HashedPassword(propertiesReadFromClient.get(HASHED_PASSWORD_KEY));
    if (!password.isValidSyntax()) {
      return "Password is not hashed correctly";
    }

    try {
      new DbUserController().createUser(user, password);
      return null;
    } catch (final IllegalStateException ise) {
      s_logger.log(Level.SEVERE, "Error creating user: " + user + ", err: " + ise.getMessage(), ise);
      return ise.getMessage();
    }
  }
}
