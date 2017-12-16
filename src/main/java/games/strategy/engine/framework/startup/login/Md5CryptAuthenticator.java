package games.strategy.engine.framework.startup.login;

import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Implements the MD5-crypt authentication protocol for peer-to-peer network games.
 *
 * <p>
 * This authenticator challenges the client with a salt and expects a response containing the MD5-crypt digest of the
 * game password and salt.
 * </p>
 */
final class Md5CryptAuthenticator {
  /**
   * Indicates the specified challenge can be processed by this authenticator.
   *
   * @param challenge The challenge sent to the client node; must not be {@code null}.
   *
   * @return {@code true} if the specified challenge can be processed by this authenticator; otherwise {@code false}.
   */
  static boolean canProcessChallenge(final Map<String, String> challenge) {
    return challenge.containsKey(ChallengePropertyNames.SALT);
  }

  @VisibleForTesting
  interface ChallengePropertyNames {
    String SALT = "Salt";
  }

  /**
   * Creates a new challenge for the server node to send to the client node.
   *
   * @return The challenge as a collection of properties to be added to the message the server node sends the client
   *         node; never {@code null}.
   */
  static Map<String, String> newChallenge() {
    return Maps.newHashMap(ImmutableMap.of(
        ChallengePropertyNames.SALT, games.strategy.util.MD5Crypt.newSalt()));
  }

  /**
   * Indicates the specified response can be processed by this authenticator.
   *
   * @param response The response received from the client node; must not be {@code null}.
   *
   * @return {@code true} if the specified response can be processed by this authenticator; otherwise {@code false}.
   */
  static boolean canProcessResponse(final Map<String, String> response) {
    return response.containsKey(ResponsePropertyNames.DIGEST);
  }

  @VisibleForTesting
  interface ResponsePropertyNames {
    String DIGEST = "Password";
  }

  /**
   * Creates a response to the specified challenge for the client node to send to the server node.
   *
   * @param password The game password; must not be {@code null}.
   * @param challenge The challenge as a collection of properties; must not be {@code null}.
   *
   * @return The response as a collection of properties to be added to the message the client node sends back to the
   *         server node; never {@code null}.
   *
   * @throws AuthenticationException If an error occurs while parsing the challenge or creating the response.
   */
  static Map<String, String> newResponse(
      final String password,
      final Map<String, String> challenge)
      throws AuthenticationException {
    final String salt = getRequiredProperty(challenge, ChallengePropertyNames.SALT);

    return Maps.newHashMap(ImmutableMap.of(
        ResponsePropertyNames.DIGEST, digest(password, salt)));
  }

  @VisibleForTesting
  static String getRequiredProperty(
      final Map<String, String> properties,
      final String name)
      throws AuthenticationException {
    final String value = properties.get(name);
    if (value == null) {
      throw new AuthenticationException(String.format("missing property '%s'", name));
    }

    return value;
  }

  private static String digest(final String password, final String salt) {
    return games.strategy.util.MD5Crypt.crypt(password, salt);
  }

  /**
   * Authenticates a client based on a previously-sent challenge.
   *
   * <p>
   * If this method does not throw an exception, the client is considered to have authenticated successfully. Otherwise,
   * the exception will contain the details of the failure. No information from the exception (including the message)
   * should be returned to the client.
   * </p>
   *
   * @param password The game password; must not be {@code null}.
   * @param challenge The challenge sent to the client node; must not be {@code null}.
   * @param response The response received from the client node; must not be {@code null}.
   *
   * @throws AuthenticationException If the client did not authenticate successfully or if an error occurs while parsing
   *         the challenge or response.
   */
  static void authenticate(
      final String password,
      final Map<String, String> challenge,
      final Map<String, String> response)
      throws AuthenticationException {
    final String salt = getRequiredProperty(challenge, ChallengePropertyNames.SALT);
    final String actualDigest = getRequiredProperty(response, ResponsePropertyNames.DIGEST);

    final String expectedDigest = digest(password, salt);
    if (!actualDigest.equals(expectedDigest)) {
      throw new AuthenticationException("authentication failed");
    }
  }
}
