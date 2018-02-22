package games.strategy.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.codec.digest.Md5Crypt.md5Crypt;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of methods for using the FreeBSD MD5-crypt password encryption algorithm.
 *
 * @see https://www.usenix.org/legacyurl/md5-crypt
 * @see https://www.systutorials.com/docs/linux/man/n-md5crypt/
 *
 * @deprecated Use SHA512(fast) or BCrypt(secure) in the future instead
 *             (kept for backwards compatibility)
 */
@Deprecated
public final class Md5Crypt {
  private static final String MAGIC = "$1$";
  private static final Pattern ENCRYPTED_PASSWORD_PATTERN =
      Pattern.compile("^" + MAGIC.replace("$", "\\$") + "([\\.\\/a-zA-Z0-9]{1,8})\\$([\\.\\/a-zA-Z0-9]{22})$");
  private static final byte[] EMPTY_KEY_BYTES = new byte[0];

  private Md5Crypt() {}

  /**
   * Encrypts the specified password using a new random salt.
   *
   * @param password The password to be encrypted.
   *
   * @return The encrypted password.
   */
  public static String crypt(final String password) {
    checkNotNull(password);

    return crypt(password, newSalt());
  }

  /**
   * Encrypts the specified password using the specified salt.
   *
   * @param password The password to be encrypted.
   * @param salt The salt. May begin with {@code $1$} and end with {@code $} followed by any number of characters.
   *        No more than eight characters will be used. If empty, a new random salt will be used.
   *
   * @return The encrypted password.
   */
  public static String crypt(final String password, final String salt) {
    checkNotNull(password);
    checkNotNull(salt);

    return md5Crypt(password.getBytes(StandardCharsets.UTF_8), MAGIC + normalizeSalt(salt));
  }

  private static String normalizeSalt(final String salt) {
    if (salt.isEmpty()) {
      return newSalt();
    }

    return String.format("%1.8s",
        salt.replaceFirst("^" + MAGIC.replace("$", "\\$"), "")
            .replaceFirst("\\$.*$", "")
            .replaceAll("[^./a-zA-Z0-9]", "."));
  }

  /**
   * Creates a new random salt that can be passed to {@link #crypt(String, String)}.
   *
   * @return A new random salt.
   */
  public static String newSalt() {
    return getSalt(md5Crypt(EMPTY_KEY_BYTES));
  }

  /**
   * Gets the hash for the specified encrypted password.
   *
   * @param encryptedPassword The encrypted password from a previous call to {@link #crypt(String, String)} whose hash
   *        is to be returned.
   *
   * @return The hash for the specified encrypted password.
   *
   * @throws IllegalArgumentException If {@code encryptedPassword} is not an MD5-crypted password.
   */
  public static String getHash(final String encryptedPassword) {
    checkNotNull(encryptedPassword);

    final Matcher matcher = ENCRYPTED_PASSWORD_PATTERN.matcher(encryptedPassword);
    checkArgument(matcher.matches(), "'" + encryptedPassword + "' is not an MD5-crypted password");
    return matcher.group(2);
  }

  /**
   * Gets the salt for the specified encrypted password.
   *
   * @param encryptedPassword The encrypted password from a previous call to {@link #crypt(String, String)} whose salt
   *        is to be returned.
   *
   * @return The salt for the specified encrypted password.
   *
   * @throws IllegalArgumentException If {@code encryptedPassword} is not an MD5-crypted password.
   */
  public static String getSalt(final String encryptedPassword) {
    checkNotNull(encryptedPassword);

    final Matcher matcher = ENCRYPTED_PASSWORD_PATTERN.matcher(encryptedPassword);
    checkArgument(matcher.matches(), "'" + encryptedPassword + "' is not an MD5-crypted password");
    return matcher.group(1);
  }

  /**
   * Indicates the specified value is a legal MD5-crypted password.
   *
   * @param value The value to test.
   *
   * @return {@code true} if the specified value is a legal MD5-crypted password; otherwise {@code false}.
   */
  public static boolean isLegalEncryptedPassword(final String value) {
    checkNotNull(value);

    return ENCRYPTED_PASSWORD_PATTERN.matcher(value).matches();
  }
}
