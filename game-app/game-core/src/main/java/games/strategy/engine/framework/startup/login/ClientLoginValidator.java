package games.strategy.engine.framework.startup.login;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IServerMessenger;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.java.ChangeOnNextMajorRelease;
import org.triplea.java.Interruptibles;
import org.triplea.util.Version;

/**
 * The server side of the peer-to-peer network game authentication protocol.
 *
 * <p>In the peer-to-peer network game authentication protocol, the server sends a challenge to the
 * client. Upon receiving the client's response, the server determines if the client knows the game
 * password and gives them access to the game if authentication is successful.
 */
public final class ClientLoginValidator implements ILoginValidator {
  static final String PASSWORD_REQUIRED_PROPERTY = "Password Required";

  private final Version engineVersion;
  private final IServerMessenger serverMessenger;
  @Nullable private String password;

  @Builder
  public ClientLoginValidator(
      @Nonnull IServerMessenger serverMessenger, @Nullable String password) {
    engineVersion = ProductVersionReader.getCurrentVersion();
    this.serverMessenger = serverMessenger;
    this.password = password;
  }

  @VisibleForTesting
  interface ErrorMessages {
    String NO_ERROR = null;
    String INVALID_PASSWORD = "Invalid password";
    String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address";
    String YOU_HAVE_BEEN_BANNED = "The host has banned you from this game";
  }

  /** Set the password required for the game. If {@code null} or empty, no password is required. */
  @ChangeOnNextMajorRelease
  public void setGamePassword(final @Nullable String password) {
    // TODO do not store the plain password, but the hash instead in the next incompatible release
    this.password = password;
  }

  @Override
  public Map<String, String> getChallengeProperties(final String username) {
    final Map<String, String> challenge = new HashMap<>();

    challenge.put("Sever Version", engineVersion.toString());

    if (!Strings.isNullOrEmpty(password)) {
      challenge.put(PASSWORD_REQUIRED_PROPERTY, Boolean.TRUE.toString());
      challenge.putAll(HmacSha512Authenticator.newChallenge());
    } else {
      challenge.put(PASSWORD_REQUIRED_PROPERTY, Boolean.FALSE.toString());
    }

    return challenge;
  }

  @Override
  @Nullable
  public String verifyConnection(
      final Map<String, String> propertiesSentToClient,
      final Map<String, String> propertiesReadFromClient,
      final String clientName,
      final String hashedMac,
      final InetSocketAddress remoteAddress) {
    final String versionString = propertiesReadFromClient.get(ClientLogin.ENGINE_VERSION_PROPERTY);
    if (versionString == null || versionString.length() > 20 || versionString.isBlank()) {
      return "Invalid version " + versionString;
    }

    // check for version
    final Version clientVersion = new Version(versionString);
    if (engineVersion.getMajor() != clientVersion.getMajor()) {
      return String.format(
          "Client is using %s but the server requires a version compatible with version %s",
          clientVersion, engineVersion);
    }

    final String remoteIp = remoteAddress.getAddress().getHostAddress();
    if (serverMessenger.isPlayerBanned(remoteIp, hashedMac)) {
      return ErrorMessages.YOU_HAVE_BEEN_BANNED;
    }

    if (hashedMac == null) {
      return ErrorMessages.UNABLE_TO_OBTAIN_MAC;
    }

    if (Boolean.TRUE.toString().equals(propertiesSentToClient.get(PASSWORD_REQUIRED_PROPERTY))) {
      final String errorMessage =
          authenticate(propertiesSentToClient, propertiesReadFromClient, password);
      if (!Objects.equals(errorMessage, ErrorMessages.NO_ERROR)) {
        // sleep on average 2 seconds
        // try to prevent flooding to guess the password
        Interruptibles.sleep((long) (4_000 * Math.random()));
        return errorMessage;
      }
    }

    return ErrorMessages.NO_ERROR;
  }

  @VisibleForTesting
  static String authenticate(
      final Map<String, String> challenge, final Map<String, String> response, String password) {
    try {
      HmacSha512Authenticator.authenticate(password, challenge, response);
      return ErrorMessages.NO_ERROR;
    } catch (final AuthenticationException e) {
      return ErrorMessages.INVALID_PASSWORD;
    }
  }
}
