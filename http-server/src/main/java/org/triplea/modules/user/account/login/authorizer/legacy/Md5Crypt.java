package org.triplea.modules.user.account.login.authorizer.legacy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.codec.digest.Md5Crypt.md5Crypt;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of methods for using the FreeBSD MD5-crypt hash algorithm.
 *
 * @see <a
 *     href="https://www.usenix.org/legacyurl/md5-crypt">https://www.usenix.org/legacyurl/md5-crypt</a>
 * @see <a href="https://www.systutorials.com/docs/linux/man/n-md5crypt/">
 *     https://www.systutorials.com/docs/linux/man/n-md5crypt/ </a>
 */
final class Md5Crypt {
  private static final String MAGIC = "$1$";
  private static final Pattern HASHED_VALUE_PATTERN =
      Pattern.compile(
          "^" + MAGIC.replace("$", "\\$") + "([\\.\\/a-zA-Z0-9]{1,8})\\$([\\.\\/a-zA-Z0-9]{22})$");
  private static final byte[] EMPTY_KEY_BYTES = new byte[0];

  private Md5Crypt() {}

  /**
   * Hashes the specified password using the specified salt.
   *
   * @param password The password to be hashed.
   * @param salt The salt. May begin with {@code $1$} and end with {@code $} followed by any number
   *     of characters. No more than eight characters will be used. If empty, a new random salt will
   *     be used.
   * @return The hashed password.
   * @deprecated MD5-crypt is not secure for hashing sensitive information, such as passwords. Use a
   *     different hashing algorithm, such as bcrypt.
   */
  @Deprecated
  public static String hashPassword(final String password, final String salt) {
    return hash(password, salt);
  }

  /**
   * Hashes the specified value using the specified salt.
   *
   * <p><b>WARNING:</b> This method should not be used to hash sensitive information.
   *
   * @param value The value to be hashed.
   * @param salt The salt. May begin with {@code $1$} and end with {@code $} followed by any number
   *     of characters. No more than eight characters will be used. If empty, a new random salt will
   *     be used.
   * @return The hashed value.
   */
  static String hash(final String value, final String salt) {
    checkNotNull(value);
    checkNotNull(salt);

    return md5Crypt(value.getBytes(StandardCharsets.UTF_8), MAGIC + normalizeSalt(salt));
  }

  private static String normalizeSalt(final String salt) {
    if (salt.isEmpty()) {
      return newSalt();
    }

    return String.format(
        "%1.8s",
        salt.replaceFirst("^" + MAGIC.replace("$", "\\$"), "")
            .replaceFirst("\\$.*$", "")
            .replaceAll("[^./a-zA-Z0-9]", "."));
  }

  /**
   * Creates a new random salt that can be passed to {@link #hash(String, String)}.
   *
   * @return A new random salt.
   */
  private static String newSalt() {
    return getSalt(md5Crypt(EMPTY_KEY_BYTES));
  }

  /**
   * Gets the salt for the specified hashed value.
   *
   * @param hashedValue The hashed value from a previous call to {@link #hash(String, String)} whose
   *     salt is to be returned.
   * @return The salt for the specified hashed value.
   * @throws IllegalArgumentException If {@code hashedValue} is not an MD5-crypt hashed value.
   */
  static String getSalt(final String hashedValue) {
    checkNotNull(hashedValue);

    final Matcher matcher = HASHED_VALUE_PATTERN.matcher(hashedValue);
    checkArgument(matcher.matches(), "'" + hashedValue + "' is not an MD5-crypt hashed value");
    return matcher.group(1);
  }
}
