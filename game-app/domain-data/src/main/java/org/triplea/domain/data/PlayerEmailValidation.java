package org.triplea.domain.data;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

/** Utility class to validates user email to check if it looks valid. */
@UtilityClass
public final class PlayerEmailValidation {

  public static boolean isValid(final String emailAddress) {
    return validate(emailAddress) == null;
  }

  /** Allow multiple fully qualified email addresses separated by spaces, or a blank string. */
  public static String validate(final String emailAddress) {
    final String quotedString = "\"(?:[^\"\\\\]|\\\\\\p{ASCII})*\"";
    @NonNls final String atom = "[^()<>@,;:\\\\\".\\[\\] \\x28\\p{Cntrl}]+";
    @NonNls final String word = "(?:" + atom + "|" + quotedString + ")";
    @NonNls final String subdomain = "(?:" + atom + "|\\[(?:[^\\[\\]\\\\]|\\\\\\p{ASCII})*\\])";
    @NonNls final String domain = subdomain + "(?:\\." + subdomain + ")*";
    @NonNls final String localPart = word + "(?:\\." + word + ")*";
    @NonNls final String email = localPart + "@" + domain;
    @NonNls final String regex = "(\\s*" + email + "\\s*)*";
    if (!emailAddress.matches(regex)) {
      return "Invalid email address";
    }
    if (emailAddress.length() > LobbyConstants.EMAIL_MAX_LENGTH) {
      return "Email address exceeds max length: " + LobbyConstants.EMAIL_MAX_LENGTH;
    }
    return null;
  }
}
