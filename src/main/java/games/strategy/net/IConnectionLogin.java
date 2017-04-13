package games.strategy.net;

import java.util.Map;

/**
 * An IConnectionLogin responds to login challenges.
 *
 * <p>
 * An IConnectionLogin is generally paired with an ILoginValidator. The validator will send a challenge string, to which
 * the
 * IConnectionLogin will respond with a key/value map of credentials. The validator will then allow the login, or return
 * an error message.
 * </p>
 */
public interface IConnectionLogin {
  /**
   * Get the properties to log in given the challenge Properties.
   */
  Map<String, String> getProperties(Map<String, String> challengProperties);

  /**
   * A notification that the login failed. The error message supplied should be shown to the user.
   */
  void notifyFailedLogin(String message);
}
