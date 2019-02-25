package org.triplea.java;

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

  // TODO: move to Strings utility
  public static boolean isInt(final String string) {
    Preconditions.checkNotNull(string);
    return string.matches("^-?\\d+$");
  }
}
