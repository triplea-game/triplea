package games.strategy.engine.framework.startup.login;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import games.strategy.engine.ClientContext;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.util.MD5Crypt;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Version;

/**
 * If we require a password, we send a public key to the client which the client uses to encrypt the password.
 * The server then decrypts this encrypted password using the associated private key.
 * If the decrypted password matches the actual password the user can join.
 */
public class ClientLoginValidator implements ILoginValidator {
  public static final String SALT_PROPERTY = "Salt";
  public static final String RANDOM_RSA_PUBLIC_KEY_PROPERTY = "RSA PUBLIC KEY";
  public static final String PASSWORD_REQUIRED_PROPERTY = "Password Required";
  static final String YOU_HAVE_BEEN_BANNED = "The host has banned you from this game";
  static final String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address";
  static final String INVALID_MAC = "Invalid mac address";
  private final IServerMessenger m_serverMessenger;
  private String m_password;
  public static final String RSA = "RSA";
  public static final String RSA_ECB_OAEPP = RSA + "/ECB/OAEPPadding";
  private static final Map<String, PrivateKey> rsaKeyMap = new HashMap<>();

  public ClientLoginValidator(final IServerMessenger serverMessenger) {
    m_serverMessenger = serverMessenger;
  }

  /**
   * Set the password required for the game, or to null if no password is required.
   */
  public void setGamePassword(final String password) {
    // TODO do not store the plain password, but the hash instead in the next incompatible release
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
      try {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
        keyGen.initialize(4096);
        final KeyPair keyPair = keyGen.generateKeyPair();
        final String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        challengeProperties.put(RANDOM_RSA_PUBLIC_KEY_PROPERTY, publicKey);
        rsaKeyMap.put(publicKey, keyPair.getPrivate());
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(RSA + " is an invalid algorithm!", e);
      }
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
      final String base64String = propertiesReadFromClient.get(ClientLogin.ENCRYPTED_PASSWORD_PROPERTY);
      if (base64String != null) {
        final String publicKey = propertiesSentToClient.get(RANDOM_RSA_PUBLIC_KEY_PROPERTY);
        try {
          final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
          cipher.init(Cipher.DECRYPT_MODE, rsaKeyMap.get(publicKey));
          if (MessageDigest.isEqual(cipher.doFinal(Base64.getDecoder().decode(base64String)), m_password.getBytes())) {
            return null;
          } else {
            ThreadUtil.sleep((int) (4000 * Math.random()));
            return "Invalid Password";
          }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
            | BadPaddingException e) {
          throw new IllegalStateException(e);
        } finally {
          rsaKeyMap.remove(publicKey);
        }
      }
      final String readPassword = propertiesReadFromClient.get(ClientLogin.PASSWORD_PROPERTY);
      if (readPassword == null) {
        return "No password";
      }
      if (!readPassword.equals(MD5Crypt.crypt(m_password, propertiesSentToClient.get(SALT_PROPERTY)))) {
        // sleep on average 2 seconds
        // try to prevent flooding to guess the password
        // TODO: verify this prevention, does this protect against parallel connections?
        ThreadUtil.sleep((int) (4000 * Math.random())); // usage of sleep is okay.
        return "Invalid password";
      }
    }
    return null;
  }
}
