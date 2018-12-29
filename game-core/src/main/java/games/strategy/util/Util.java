package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Some utility methods for dealing with collections.
 */
public class Util {

  private static final String SHA_512 = "SHA-512";

  private Util() {}

  /**
   * allow multiple fully qualified email addresses separated by spaces, or a blank string.
   */
  public static boolean isMailValid(final String emailAddress) {
    final String quotedString = "\"(?:[^\"\\\\]|\\\\\\p{ASCII})*\"";
    final String atom = "[^()<>@,;:\\\\\".\\[\\] \\x28\\p{Cntrl}]+";
    final String word = "(?:" + atom + "|" + quotedString + ")";
    final String subdomain = "(?:" + atom + "|\\[(?:[^\\[\\]\\\\]|\\\\\\p{ASCII})*\\])";
    final String domain = subdomain + "(?:\\." + subdomain + ")*";
    final String localPart = word + "(?:\\." + word + ")*";
    final String email = localPart + "@" + domain;
    // String regex = "(\\s*[\\w\\.-]+@\\w+\\.[\\w\\.]+\\s*)*";
    final String regex = "(\\s*" + email + "\\s*)*";
    return emailAddress.matches(regex);
  }

  public static String newUniqueTimestamp() {
    final long time = System.currentTimeMillis();
    while (time == System.currentTimeMillis()) {
      Interruptibles.sleep(1);
    }
    return "" + System.currentTimeMillis();
  }

  /**
   * Creates a hash of the given String based on the SHA-512 algorithm.
   *
   * @param input The input String to hash.
   * @return A hashed hexadecimal String of the input.
   */
  public static String sha512(final String input) {
    Preconditions.checkNotNull(input);
    try {
      return BaseEncoding.base16()
          .encode(MessageDigest.getInstance(SHA_512).digest(input.getBytes(StandardCharsets.UTF_8))).toLowerCase();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(SHA_512 + " is not supported!", e);
    }
  }

  /**
   * Returns a predicate that represents the logical negation of the specified predicate.
   *
   * @param p The predicate to negate.
   *
   * @return A predicate that represents the logical negation of the specified predicate.
   */
  public static <T> Predicate<T> not(final Predicate<T> p) {
    checkNotNull(p);

    return p.negate();
  }

  public static String getFromSetting(final ClientSetting<char[]> setting) {
    final char[] charArray = setting.getValueOrThrow();
    try {
      return new String(charArray);
    } finally {
      Arrays.fill(charArray, '\0');
    }
  }

  public static boolean isInt(final String string) {
    Preconditions.checkNotNull(string);
    return string.matches("^\\d+$");
  }
}
