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
    if (emailAddress.length() > LobbyConstants.EMAIL_INPUT_FIELD_MAX_LENGTH) {
      return "Total length of all email address(es) exceeds general max length: "
          + LobbyConstants.EMAIL_INPUT_FIELD_MAX_LENGTH;
    }
    // Split at every space that was not quoted since addresses like "Email Name"123@some.com are
    // valid.
    String[] addresses = emailAddress.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    StringBuilder sbInvalidAddresses = new StringBuilder();
    StringBuilder sbTooLongAddresses = new StringBuilder();
    for (int i = 0; i < addresses.length; i++) {
      if (!addresses[i].matches(email)) {
        if (sbInvalidAddresses.isEmpty()) {
          sbInvalidAddresses.append(addresses[i]);
        } else {
          sbInvalidAddresses.append("; " + addresses[i]);
        }
      }
      if (addresses[i].length() > LobbyConstants.EMAIL_MAX_LENGTH) {
        if (sbTooLongAddresses.isEmpty()) {
          sbTooLongAddresses.append(addresses[i]);
        } else {
          sbTooLongAddresses.append("; " + addresses[i]);
        }
      }
    }
    String errorMessage = "";
    if (!sbInvalidAddresses.isEmpty()) {
      errorMessage = "The following email addresses are invalid: " + sbInvalidAddresses + ".";
    }
    if (!sbTooLongAddresses.isEmpty()) {
      errorMessage = errorMessage.isEmpty() ? "" : errorMessage + "\n";
      errorMessage +=
          "The following email addresses exceed the max length "
              + LobbyConstants.EMAIL_MAX_LENGTH
              + ": "
              + sbTooLongAddresses
              + ".";
    }
    if (!errorMessage.isEmpty()) {
      return errorMessage;
    }
    return null;
  }
}
