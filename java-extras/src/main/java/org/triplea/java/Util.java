package org.triplea.java;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

import com.google.common.base.Preconditions;

/**
 * Some utility methods for dealing with collections.
 */
public class Util {


  private Util() {}

  /**
   * allow multiple fully qualified email addresses separated by spaces, or a blank string.
   */
  // TODO: move to an EmailUtil class or to Strings utility.
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
   * Returns a predicate that represents the logical negation of the specified predicate.
   *
   * @param p The predicate to negate.
   *
   * @return A predicate that represents the logical negation of the specified predicate.
   *
   * @deprecated Call NegationPredicate instead
   */
  @Deprecated
  public static <T> Predicate<T> not(final Predicate<T> p) {
    checkNotNull(p);

    return p.negate();
  }

  // TODO: move to Strings utility
  public static boolean isInt(final String string) {
    Preconditions.checkNotNull(string);
    return string.matches("^-?\\d+$");
  }
}
