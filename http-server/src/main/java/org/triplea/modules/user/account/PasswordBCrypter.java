package org.triplea.modules.user.account;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

public class PasswordBCrypter implements Function<String, String> {
  // TODO: Md5-Deprecation This value needs to be kept in sync with Sha512Hasher
  private static final String PSEUDO_SALT = "TripleA";
  private static final String SHA_512 = "SHA-512";

  @Override
  public String apply(final String password) {
    // TODO: Md5-Deprecation remove hashing here, move to client-side
    final String hashed = hashPasswordWithSalt(password);
    return hashPassword(hashed);
  }

  /**
   * This is a helper method designed to simplify the bcrypt API and hide some of the constants
   * involved. This method generates a hash with 10 rounds. This number is arbitrary and might
   * increase at a later time.
   *
   * @param password The string to apply the bcrypt algorithm to.
   * @return A hashed password using a randomly generated bcrypt salt.
   */
  public static String hashPassword(final String password) {
    return BCrypt.with(LongPasswordStrategies.none()).hashToString(10, password.toCharArray());
  }

  /**
   * Checks of the provided password does match the existing hash. NOTE: Any passwords longer than
   * 72 bytes (UTF-8) will result in the same hash as the version trimmed to 72 bytes.
   *
   * @param password The password to check.
   * @param hash The hash to verify the password against.
   * @return True if the password matches the hash, false otherwise.
   */
  public static boolean verifyHash(final String password, final String hash) {
    return BCrypt.verifyer(null, LongPasswordStrategies.none())
        .verify(password.toCharArray(), hash.toCharArray())
        .verified;
  }

  /**
   * Legacy security precaution. When the lobby still relied on plain TCP sockets, a pseudo-security
   * for passwords was implemented to prevent them from leaking out into the internet. This
   * algorithm was vulnerable to a MITM-attack, so in order to keep the user safe in case they
   * re-used their TripleA password anywhere else, we took the password, prepended a fixed String
   * provided by {@link #PSEUDO_SALT}, and SHA-512 hashed the whole thing before sending it over the
   * network. This way an attacker would have to explicitly pre-calculate hashes for TripleA
   * exclusively in order to brute-force the original password, which hopefully made the thing
   * annoying enough for most people to try.
   */
  public static String hashPasswordWithSalt(final String password) {
    Preconditions.checkNotNull(password);
    return sha512(PSEUDO_SALT + password);
  }

  /** Creates a SHA-512 hash of the given String. */
  @VisibleForTesting
  static String sha512(final String input) {
    try {
      return BaseEncoding.base16()
          .encode(MessageDigest.getInstance(SHA_512).digest(input.getBytes(StandardCharsets.UTF_8)))
          .toLowerCase();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(SHA_512 + " is not supported!", e);
    }
  }
}
