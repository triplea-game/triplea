package games.strategy.engine.lobby.server.login;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.common.base.Strings;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.db.BadWordController;
import games.strategy.engine.lobby.server.db.BannedMacController;
import games.strategy.engine.lobby.server.db.BannedUsernameController;
import games.strategy.engine.lobby.server.db.DbUserController;
import games.strategy.engine.lobby.server.db.HashedPassword;
import games.strategy.engine.lobby.server.db.UserDao;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.ILoginValidator;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Tuple;
import games.strategy.util.Version;

public class LobbyLoginValidator implements ILoginValidator {
  static final String THATS_NOT_A_NICE_NAME = "That's not a nice name.";
  private static final String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
  private static final String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
  private static final String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address.";
  private static final String INVALID_MAC = "Invalid mac address.";
  private static final Logger s_logger = Logger.getLogger(LobbyLoginValidator.class.getName());
  public static final String LOBBY_VERSION = "LOBBY_VERSION";
  public static final String REGISTER_NEW_USER_KEY = "REGISTER_USER";
  public static final String ANONYMOUS_LOGIN = "ANONYMOUS_LOGIN";
  public static final String LOBBY_WATCHER_LOGIN = "LOBBY_WATCHER_LOGIN";
  public static final String HASHED_PASSWORD_KEY = "HASHEDPWD";
  public static final String ENCRYPTED_PASSWORD_KEY = "RSAPWD";
  public static final String RSA_PUBLIC_KEY = "RSAPUBLICKEY";
  public static final String EMAIL_KEY = "EMAIL";
  public static final String SALT_KEY = "SALT";
  public static final String RSA = "RSA";
  private static final Map<String, PrivateKey> rsaKeyMap = new HashMap<>();

  public LobbyLoginValidator() {}

  @Override
  public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress) {
    // we need to give the user the salt key for the username
    final Map<String, String> rVal = new HashMap<>();
    final HashedPassword password = new DbUserController().getPassword(userName);
    if (password != null && Strings.emptyToNull(password.value) != null) {
      rVal.put(SALT_KEY, password.isBcrypted() ? "" : MD5Crypt.getSalt(MD5Crypt.MAGIC, password.value));
      try {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
        keyGen.initialize(4096);
        final KeyPair keyPair = keyGen.generateKeyPair();
        final String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        rVal.put(RSA_PUBLIC_KEY, publicKey);
        rsaKeyMap.put(publicKey, keyPair.getPrivate());
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(RSA + " is an invalid algorithm!", e);
      }
    }
    return rVal;
  }

  @Override
  public String verifyConnection(final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient, final String clientName, final String clientMac,
      final SocketAddress remoteAddress) {
    final String error =
        verifyConnectionInternal(propertiesSentToClient, propertiesReadFromClient, clientName, clientMac);
    if (error != null) {
      s_logger.info("Bad login attempt from " + remoteAddress + " for user " + clientName + " error:" + error);
      AccessLog.failedLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress(), error);
    } else {
      s_logger.info("Successful login from:" + remoteAddress + " for user:" + clientName);
      AccessLog.successfulLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress());
    }
    return error;
  }

  private static String verifyConnectionInternal(final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient,
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
      return createUser(propertiesSentToClient, propertiesReadFromClient, clientName);
    }
    if (propertiesReadFromClient.containsKey(ANONYMOUS_LOGIN)) {
      return anonymousLogin(propertiesReadFromClient, clientName);
    } else {
      return validatePassword(propertiesSentToClient, propertiesReadFromClient, clientName);
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

  private static String validatePassword(final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient, final String clientName) {
    final String errorMessage = "Incorrect username or password";
    final UserDao userDao = new DbUserController();
    final HashedPassword hashedPassword = userDao.getPassword(clientName);
    if (hashedPassword == null) {
      return errorMessage;
    }
    final String base64 = propertiesReadFromClient.get(ENCRYPTED_PASSWORD_KEY);
    if (base64 != null) {
      try {
        final Cipher cipher = Cipher.getInstance(RSA);
        final String publicKey = propertiesSentToClient.get(RSA_PUBLIC_KEY);
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyMap.get(publicKey));
        final String simpleHashedPassword =
            new String(cipher.doFinal(Base64.getDecoder().decode(base64)), StandardCharsets.UTF_8);
        if (hashedPassword.isBcrypted()) {
          return userDao.login(clientName, simpleHashedPassword) ? null : errorMessage;
        } else if (userDao.login(clientName, new HashedPassword(propertiesReadFromClient.get(HASHED_PASSWORD_KEY)))) {
          userDao.updateUser(userDao.getUserByName(clientName), simpleHashedPassword);
          rsaKeyMap.remove(publicKey);
          return null;
        } else {
          return errorMessage;
        }
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
          | BadPaddingException e) {
        throw new IllegalStateException(e);
      }
    }
    if (!userDao.login(clientName, new HashedPassword(propertiesReadFromClient.get(HASHED_PASSWORD_KEY)))) {
      if (hashedPassword.isBcrypted()) {
        return "You need to login with a newer version of TripleA";
      }
      return errorMessage;
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

      if (!DBUser.isValidUserName(hostName)) {
        return DBUser.getUserNameValidationErrorMessage(hostName);
      }
    } else {
      if (DBUser.isValidUserName(userName)) {
        return null;
      } else {
        return DBUser.getUserNameValidationErrorMessage(userName);
      }
    }
    return null;
  }

  private static String createUser(final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient, final String userName) {
    final DBUser user = new DBUser(
        new DBUser.UserName(userName),
        new DBUser.UserEmail(propertiesReadFromClient.get(EMAIL_KEY)));

    if (!user.isValid()) {
      return user.getValidationErrorMessage();
    }

    if (new DbUserController().doesUserExist(user.getName())) {
      return "That user name has already been taken";
    }
    final String base64 = propertiesReadFromClient.get(ENCRYPTED_PASSWORD_KEY);
    if (base64 != null) {
      try {
        final Cipher cipher = Cipher.getInstance(RSA);
        final String publicKey = propertiesSentToClient.get(RSA_PUBLIC_KEY);
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyMap.get(publicKey));
        new DbUserController().createUser(user,
            new String(cipher.doFinal(Base64.getDecoder().decode(base64)), StandardCharsets.UTF_8));
        return null;
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
          | BadPaddingException e) {
        return e.getMessage();
      }
    }

    final HashedPassword password = new HashedPassword(propertiesReadFromClient.get(HASHED_PASSWORD_KEY));
    if (!password.isValidSyntax()) {
      return "Password is not hashed correctly";
    }

    try {
      new DbUserController().createUser(user, password);
      return null;
    } catch (final Exception e) {
      return e.getMessage();
    }
  }
}
