package org.triplea.server.user.account;

import at.favre.lib.crypto.bcrypt.BCrypt;
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
    return BCrypt.withDefaults().hashToString(10, hashed.toCharArray());
  }

  @VisibleForTesting
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
