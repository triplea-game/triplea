package games.strategy.net;

import java.net.InetSocketAddress;
import java.util.Map;
import javax.annotation.Nullable;

/** Code to validate a login attempt. */
public interface ILoginValidator {
  /**
   * The challenge properties to send to the client. The client will be sent the challenge
   * properties, and will be expected to return a properties object to validate its connection.
   */
  Map<String, String> getChallengeProperties(String username);

  /**
   * Validates a login attempt.
   *
   * @param propertiesReadFromClient Client properties written by the client after receiving the
   *     challenge string.
   * @param remoteAddress The remote address.
   * @param clientName - the user name given by the client
   * @return - null if the attempt was successful, an error message otherwise
   */
  @Nullable
  String verifyConnection(
      Map<String, String> propertiesSentToClient,
      Map<String, String> propertiesReadFromClient,
      String clientName,
      String clientMac,
      InetSocketAddress remoteAddress);
}
