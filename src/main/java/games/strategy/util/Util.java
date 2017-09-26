package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;

/**
 * Some utility methods for dealing with collections.
 */
public class Util {

  private static final String SHA_512 = "SHA-512";

  /**
   * return a such that a exists in c1 and a exists in c2.
   * always returns a new collection.
   */
  public static <T> List<T> intersection(final Collection<T> c1, final Collection<T> c2) {
    if (c1 == null || c2 == null) {
      return new ArrayList<>();
    }
    return c1.stream().filter(c2::contains).collect(Collectors.toList());
  }

  /**
   * Equivalent to !intersection(c1,c2).isEmpty(), but more efficient.
   *
   * @return true if some element in c1 is in c2
   */
  public static <T> boolean someIntersect(final Collection<T> c1, final Collection<T> c2) {
    return c1.stream().anyMatch(c2::contains);
  }

  /**
   * Returns a such that a exists in c1 but not in c2.
   * Always returns a new collection.
   */
  public static <T> List<T> difference(final Collection<T> c1, final Collection<T> c2) {
    if (c1 == null || c1.size() == 0) {
      return new ArrayList<>(0);
    }
    if (c2 == null || c2.size() == 0) {
      return new ArrayList<>(c1);
    }
    return c1.stream().filter(not(c2::contains)).collect(Collectors.toList());
  }

  /**
   * true if for each a in c1, a exists in c2,
   * and if for each b in c2, b exist in c1
   * and c1 and c2 are the same size.
   * Note that (a,a,b) (a,b,b) are equal.
   */
  public static <T> boolean equals(final Collection<T> c1, final Collection<T> c2) {
    if (c1 == null || c2 == null) {
      return c1 == c2;
    }
    if (c1.size() != c2.size()) {
      return false;
    }
    if (c1 == c2) {
      return true;
    }
    return c2.containsAll(c1) && c1.containsAll(c2);
  }

  /** Creates new Util. */
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

  public static String createUniqueTimeStamp() {
    final long time = System.currentTimeMillis();
    while (time == System.currentTimeMillis()) {
      ThreadUtil.sleep(1);
    }
    return "" + System.currentTimeMillis();
  }

  public static <T> void reorder(final List<T> reorder, final List<T> order) {
    final IntegerMap<T> map = new IntegerMap<>();
    for (final T o : order) {
      map.put(o, order.indexOf(o));
    }
    Collections.sort(reorder, (o1, o2) -> {
      // get int returns 0 if no value
      final int v1 = map.getInt(o1);
      final int v2 = map.getInt(o2);
      if (v1 > v2) {
        return 1;
      } else if (v1 == v2) {
        return 0;
      } else {
        return -1;
      }
    });
  }

  /**
   * Creates a hash of the given String based on the SHA-512 algorithm.
   * 
   * @param input The input String to hash.
   * @return A hashed hexadecimal String of the input.
   */
  public static String sha512(String input) {
    Preconditions.checkNotNull(input);
    try {
      return BaseEncoding.base16()
          .encode(MessageDigest.getInstance(SHA_512).digest(input.getBytes(StandardCharsets.UTF_8))).toLowerCase();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(SHA_512 + " is not supported!", e);
    }
  }

  /**
   * Returns a predicate that represents the logical negation of the specified predicate.
   *
   * @param p The predicate to negate; must not be {@code null}.
   *
   * @return A predicate that represents the logical negation of the specified predicate; never {@code null}.
   */
  public static <T> Predicate<T> not(final Predicate<T> p) {
    checkNotNull(p);

    return p.negate();
  }

  /**
   * Workaround for http://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8187708
   * Essentially a better version of {@link Date#from(Instant)}.
   */
  public static Date toRealDate(final Instant instant) {
    if (instant.getEpochSecond() < 0 && instant.getNano() > 0) {
      long millis = (instant.getEpochSecond() + 1) * 1000;
      long adjustment = instant.getNano() / 1000_000 - 1000;
      return new Date(millis + adjustment);
    } else {
      long millis = instant.getEpochSecond() * 1000;
      return new Date(millis + (instant.getNano() / 1000_000));
    }
  }
}
