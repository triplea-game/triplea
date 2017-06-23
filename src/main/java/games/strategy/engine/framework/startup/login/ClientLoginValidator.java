package games.strategy.engine.framework.startup.login;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import games.strategy.engine.ClientContext;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.util.MD5Crypt;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Version;

/**
 * If we require a password, then we challenge the client with a salt value, the salt
 * being different for each login attempt. . The client hashes the password entered by
 * the user with this salt, and sends it back to us. This prevents the password from
 * travelling over the network in plain text, and also prevents someone listening on
 * the connection from getting enough information to log in (since the salt will change
 * on the next login attempt)
 */
public class ClientLoginValidator implements ILoginValidator {
  public static final String SALT_PROPERTY = "Salt";
  public static final String PASSWORD_REQUIRED_PROPERTY = "Password Required";
  static final String YOU_HAVE_BEEN_BANNED = "The host has banned you from this game";
  static final String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address";
  static final String INVALID_MAC = "Invalid mac address";
  private final IServerMessenger m_serverMessenger;
  private String m_password;

  public ClientLoginValidator(final IServerMessenger serverMessenger) {
    m_serverMessenger = serverMessenger;
  }

  /**
   * Set the password required for the game, or to null if no password is required.
   */
  public void setGamePassword(final String password) {
    m_password = password;
  }

  @Override
  public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress) {
    final Map<String, String> challengeProperties = new HashMap<>();
    challengeProperties.put("Sever Version", ClientContext.engineVersion().toString());
    if (m_password != null) {
      /**
       * Get a new random salt.
       */
      final String encryptedPassword = MD5Crypt.crypt(m_password);
      challengeProperties.put(SALT_PROPERTY, MD5Crypt.getSalt(MD5Crypt.MAGIC, encryptedPassword));
      challengeProperties.put(PASSWORD_REQUIRED_PROPERTY, Boolean.TRUE.toString());
    } else {
      challengeProperties.put(PASSWORD_REQUIRED_PROPERTY, Boolean.FALSE.toString());
    }
    return challengeProperties;
  }

  @Override
  public String verifyConnection(final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient, final String clientName, final String hashedMac,
      final SocketAddress remoteAddress) {
    final String versionString = propertiesReadFromClient.get(ClientLogin.ENGINE_VERSION_PROPERTY);
    if (versionString == null || versionString.length() > 20 || versionString.trim().length() == 0) {
      return "Invalid version " + versionString;
    }
    // check for version
    final Version clientVersion = new Version(versionString);
    if (!ClientContext.engineVersion().equals(clientVersion, false)) {
      return "Client is using " + clientVersion + " but server requires version "
          + ClientContext.engineVersion();
    }
    final String realName = clientName.split(" ")[0];
    if (m_serverMessenger.isUsernameMiniBanned(realName)) {
      return YOU_HAVE_BEEN_BANNED;
    }
    final String remoteIp = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
    if (m_serverMessenger.isIpMiniBanned(remoteIp)) {
      return YOU_HAVE_BEEN_BANNED;
    }
    if (hashedMac == null) {
      return UNABLE_TO_OBTAIN_MAC;
    }
    if (hashedMac.length() != 28 || !hashedMac.startsWith(MD5Crypt.MAGIC + "MH$")
        || !hashedMac.matches("[0-9a-zA-Z$./]+")) {
      // Must have been tampered with
      return INVALID_MAC;
    }
    if (m_serverMessenger.isMacMiniBanned(hashedMac)) {
      return YOU_HAVE_BEEN_BANNED;
    }
    if (propertiesSentToClient.get(PASSWORD_REQUIRED_PROPERTY).equals(Boolean.TRUE.toString())) {
      final String readPassword = propertiesReadFromClient.get(ClientLogin.PASSWORD_PROPERTY);
      if (readPassword == null) {
        return "No password";
      }
      if (!readPassword.equals(MD5Crypt.crypt(m_password, propertiesSentToClient.get(SALT_PROPERTY)))) {
        // sleep on average 2 seconds
        // try to prevent flooding to guess the password
        // TODO: verify this prevention, does this protect against parallel connections?
        ThreadUtil.sleep(4000 * Math.random()); // usage of sleep is okay.
        return "Invalid password";
      }
    }
    return null;
  }
}
