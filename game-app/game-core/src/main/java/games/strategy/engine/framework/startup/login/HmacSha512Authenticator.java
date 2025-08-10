package games.strategy.engine.framework.startup.login;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Implements the HMAC-SHA512 authentication protocol for peer-to-peer network games.
 *
 * <p>This authenticator challenges the client with a nonce and salt and expects a response
 * containing the HMAC-SHA512 digest of the nonce using a key derived from the game password and
 * salt.
 */
final class HmacSha512Authenticator {

  /**
   * We do not expire nonce values, because they are long-lived we use a larger value (rather than
   * say a more minimal 8 bytes).
   * https://security.stackexchange.com/questions/1952/how-long-should-a-random-nonce-be
   */
  private static final int LARGE_NONCE_LENGTH = 64;

  /**
   * Salt length is chosen to match hash output size. Reference:
   * https://crackstation.net/hashing-security.htm#salt
   */
  private static final int HASH_OUTPUT_SIZE = 64;

  private HmacSha512Authenticator() {}

  @VisibleForTesting
  interface ChallengePropertyNames {
    String NONCE = "authenticator/hmac-sha512/nonce";
    String SALT = "authenticator/hmac-sha512/salt";
  }

  /**
   * Creates a new challenge for the server node to send to the client node.
   *
   * @return The challenge as a collection of properties to be added to the message the server node
   *     sends the client node; never {@code null}.
   */
  static Map<String, String> newChallenge() {
    return Maps.newHashMap(
        ImmutableMap.<String, String>builder()
            .put(encodeProperty(ChallengePropertyNames.NONCE, newRandomBytes(LARGE_NONCE_LENGTH)))
            .put(encodeProperty(ChallengePropertyNames.SALT, newRandomBytes(HASH_OUTPUT_SIZE)))
            .build());
  }

  private static Map.Entry<String, String> encodeProperty(final String name, final byte[] value) {
    return Maps.immutableEntry(name, Base64.getEncoder().encodeToString(value));
  }

  private static byte[] newRandomBytes(final int numberOfBytes) {
    // It is sufficient to use the default non-strong secure PRNG for the platform because the
    // secrets we are protecting
    // are short lived.
    //
    // https://stackoverflow.com/questions/27622625/securerandom-with-nativeprng-vs-sha1prng
    final SecureRandom secureRandom = new SecureRandom();

    final byte[] bytes = new byte[numberOfBytes];
    secureRandom.nextBytes(bytes);
    return bytes;
  }

  @VisibleForTesting
  interface ResponsePropertyNames {
    String DIGEST = "authenticator/hmac-sha512/digest";
  }

  /**
   * Creates a response to the specified challenge for the client node to send to the server node.
   *
   * @param password The game password; must not be {@code null}.
   * @param challenge The challenge as a collection of properties; must not be {@code null}.
   * @return The response as a collection of properties to be added to the message the client node
   *     sends back to the server node; never {@code null}.
   * @throws AuthenticationException If an error occurs while parsing the challenge or creating the
   *     response.
   */
  static Map<String, String> newResponse(final String password, final Map<String, String> challenge)
      throws AuthenticationException {
    final byte[] nonce = decodeOptionalProperty(challenge, ChallengePropertyNames.NONCE);
    final byte[] salt = decodeOptionalProperty(challenge, ChallengePropertyNames.SALT);
    if (nonce == null || salt == null) {
      return Map.of();
    }

    try {
      return ImmutableMap.copyOf(
          Maps.newHashMap(
              ImmutableMap.<String, String>builder()
                  .put(encodeProperty(ResponsePropertyNames.DIGEST, digest(password, salt, nonce)))
                  .build()));
    } catch (final GeneralSecurityException e) {
      throw new AuthenticationException("security framework failure when creating response", e);
    }
  }

  @VisibleForTesting
  @Nullable
  static byte[] decodeOptionalProperty(final Map<String, String> properties, final String name)
      throws AuthenticationException {
    final String value = properties.get(name);
    if (value == null) {
      return null;
    }

    try {
      return Base64.getDecoder().decode(value);
    } catch (final IllegalArgumentException e) {
      throw new AuthenticationException(
          String.format("malformed value for property '%s'", name), e);
    }
  }

  private static byte[] digest(final String password, final byte[] salt, final byte[] nonce)
      throws GeneralSecurityException {
    final Mac mac = Mac.getInstance("HmacSHA512");
    mac.init(newSecretKey(password, salt));
    return mac.doFinal(nonce);
  }

  private static SecretKey newSecretKey(final String password, final byte[] salt)
      throws GeneralSecurityException {
    // 20,000 iterations was empirically determined to provide good performance on circa 2014
    // hardware for
    // PBKDF2WithHmacSHA512.
    //
    // https://security.stackexchange.com/questions/3959/recommended-of-iterations-when-using-pkbdf2-sha256
    final int iterationCount = 20_000;

    // Per RFC 4868, the key length for HmacSHA512 should be between 512 bits (L) and 1,024 bits
    // (B). However, some
    // references insist that, in the context of current computing power to break crypto, 128 bits
    // should be
    // sufficient.
    //
    // https://security.stackexchange.com/a/96176/136686
    final int keyLengthInBits = 512;

    final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
    final KeySpec keySpec =
        new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLengthInBits);
    return secretKeyFactory.generateSecret(keySpec);
  }

  /**
   * Authenticates a client based on a previously-sent challenge.
   *
   * <p>If this method does not throw an exception, the client is considered to have authenticated
   * successfully. Otherwise, the exception will contain the details of the failure. No information
   * from the exception (including the message) should be returned to the client.
   *
   * @param password The game password; must not be {@code null}.
   * @param challenge The challenge sent to the client node; must not be {@code null}.
   * @param response The response received from the client node; must not be {@code null}.
   * @throws AuthenticationException If the client did not authenticate successfully or if an error
   *     occurs while parsing the challenge or response.
   */
  static void authenticate(
      final String password,
      final Map<String, String> challenge,
      final Map<String, String> response)
      throws AuthenticationException {
    final byte[] nonce = decodeRequiredProperty(challenge, ChallengePropertyNames.NONCE);
    final byte[] salt = decodeRequiredProperty(challenge, ChallengePropertyNames.SALT);
    final byte[] actualDigest = decodeRequiredProperty(response, ResponsePropertyNames.DIGEST);

    try {
      final byte[] expectedDigest = digest(password, salt, nonce);
      if (!MessageDigest.isEqual(expectedDigest, actualDigest)) {
        throw new AuthenticationException("authentication failed");
      }
    } catch (final GeneralSecurityException e) {
      throw new AuthenticationException("security framework failure during authentication", e);
    }
  }

  @VisibleForTesting
  static byte[] decodeRequiredProperty(final Map<String, String> properties, final String name)
      throws AuthenticationException {
    final byte[] value = decodeOptionalProperty(properties, name);
    if (value == null) {
      throw new AuthenticationException(String.format("missing property '%s'", name));
    }

    return value;
  }
}
