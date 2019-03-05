package org.triplea.java;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A collection of useful methods for working with instances of {@link String}.
 */
public final class StringUtils {
  private StringUtils() {}

  /**
   * Returns {@code value} with the first character capitalized. An empty string is returned unchanged.
   */
  public static String capitalize(final String value) {
    checkNotNull(value);

    return value.isEmpty() ? value : (value.substring(0, 1).toUpperCase() + value.substring(1));
  }

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
}
