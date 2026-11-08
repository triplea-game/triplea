package games.strategy.net;

import java.util.Map;

/**
 * An IConnectionLogin responds to login challenges.
 *
 * <p>An IConnectionLogin is generally paired with an ILoginValidator. The validator will send a
 * challenge string, to which the IConnectionLogin will respond with a key/value map of credentials.
 * The validator will then allow the login or return an error message.
 */
public interface IConnectionLogin {
  /** Get the properties to log in given the challenge Properties. */
  Map<String, String> getProperties(Map<String, String> challengeProperties);

  /**
   * Called when the server assigns a name that does not start with the requested name, meaning this
   * machine is already logged in under {@code assignedName}.
   */
  void onAssignedDifferentName(String assignedName);
}
