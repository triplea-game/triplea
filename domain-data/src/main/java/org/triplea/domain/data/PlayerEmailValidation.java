package org.triplea.domain.data;

import lombok.NoArgsConstructor;

/** Utility class to validates user email to check if it looks valid. */
@NoArgsConstructor
public final class PlayerEmailValidation {

  public static boolean isValid(final String emailAddress) {
    return validate(emailAddress) == null;
  }

  /** Allow multiple fully qualified email addresses separated by spaces, or a blank string. */
  public static String validate(final String emailAddress) {
    final String quotedString = "\"(?:[^\"\\\\]|\\\\\\p{ASCII})*\"";
    final String atom = "[^()<>@,;:\\\\\".\\[\\] \\x28\\p{Cntrl}]+";
    final String word = "(?:" + atom + "|" + quotedString + ")";
    final String subdomain = "(?:" + atom + "|\\[(?:[^\\[\\]\\\\]|\\\\\\p{ASCII})*\\])";
    final String domain = subdomain + "(?:\\." + subdomain + ")*";
    final String localPart = word + "(?:\\." + word + ")*";
    final String email = localPart + "@" + domain;
    final String regex = "(\\s*" + email + "\\s*)*";
    return emailAddress.matches(regex) ? null : "Invalid email address";
  }
}
