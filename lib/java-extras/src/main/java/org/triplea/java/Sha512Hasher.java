package org.triplea.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NonNls;

/**
 * A class which implements the TripleA-Lobby-Login authentication system using RSA encryption for
 * passwords.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Sha512Hasher {
  @NonNls private static final String PSEUDO_SALT = "TripleA";
  @NonNls private static final String SHA_512 = "SHA-512";

  /**
   * Creates a SHA-512 hash of a given String with a salt. <br>
   * The server doesn't need to know the actual password, so this hash essentially replaces the real
   * password. In case any other server authentication system SHA-512 hashes passwords before
   * sending them, we are applying a 'TripleA' prefix to the given String before hashing. This way
   * the hash cannot be used on other websites even if the password and the authentication system is
   * the same.
   *
   * @param password The input String to hash.
   * @return A hashed hexadecimal String of the input.
   */
  public static String hashPasswordWithSalt(final String password) {
    if (password.isBlank()) {
      return password;
    }
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
