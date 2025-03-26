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
    if (emailAddress.length() > LobbyConstants.EMAIL_GENERAL_LENGTH) {
      return "Total length of all email address(es) exceeds general max length: "
          + LobbyConstants.EMAIL_GENERAL_LENGTH;
    }
    String[] addresses = emailAddress.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    String invalidAddresses = "";
    String tooLongAddresses = "";
    for (int i = 0; i < addresses.length; i++) {
      if (!addresses[i].matches(email)) {
        invalidAddresses =
            invalidAddresses.isEmpty() ? addresses[i] : invalidAddresses + "; " + addresses[i];
      }
      if (addresses[i].length() > LobbyConstants.EMAIL_MAX_LENGTH) {
        tooLongAddresses =
            tooLongAddresses.isEmpty() ? addresses[i] : tooLongAddresses + "; " + addresses[i];
      }
    }
    if (!invalidAddresses.isEmpty() && !tooLongAddresses.isEmpty()) {
      return "The following email addresses are invalid: "
          + invalidAddresses
          + "\nThe following email addresses exceed the max length "
          + LobbyConstants.EMAIL_MAX_LENGTH
          + ": "
          + tooLongAddresses;
    }
    if (!invalidAddresses.isEmpty()) {
      return "The following email addresses are invalid: " + invalidAddresses;
    }
    if (!tooLongAddresses.isEmpty()) {
      return "The following email addresses exceed the max length "
          + LobbyConstants.EMAIL_MAX_LENGTH
          + ": "
          + tooLongAddresses;
    }
    return null;
  }
}
