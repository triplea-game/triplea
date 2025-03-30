package org.triplea.domain.data;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

/** Utility class to validates user email to check if it looks valid. */
@UtilityClass
public final class PlayerEmailValidation {

  @NonNls private static final String QUOTED_STRING_REGEX = "\"(?:[^\"\\\\]|\\\\\\p{ASCII})*\"";
  @NonNls private static final String ATOM_REGEX = "[^()<>@,;:\\\\\".\\[\\] \\x28\\p{Cntrl}]+";

  @NonNls
  private static final String WORD_REGEX = "(?:" + ATOM_REGEX + "|" + QUOTED_STRING_REGEX + ")";

  @NonNls
  private static final String SUBDOMAIN_REGEX =
      "(?:" + ATOM_REGEX + "|\\[(?:[^\\[\\]\\\\]|\\\\\\p{ASCII})*\\])";

  @NonNls
  private static final String DOMAIN_REGEX = SUBDOMAIN_REGEX + "(?:\\." + SUBDOMAIN_REGEX + ")*";

  @NonNls private static final String LOCAL_PART_REGEX = WORD_REGEX + "(?:\\." + WORD_REGEX + ")*";
  @NonNls private static final String EMAIL_REGEX = LOCAL_PART_REGEX + "@" + DOMAIN_REGEX;

  public static boolean isValid(final String emailAddress) {
    return validate(emailAddress) == null;
  }

  public static boolean areValid(final String emailAddresses) {
    // Split at every space that was not quoted since addresses like "Email Name"123@some.com are
    // valid.
    String[] addresses = emailAddresses.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    for (String address : addresses) {
      if (!isValid(address)) {
        return false;
      }
    }
    return true;
  }

  /** Allow multiple fully qualified email addresses separated by spaces, or a blank string. */
  public static String validate(final String emailAddress) {
    if (emailAddress == null) {
      return null;
    }
    if (emailAddress.length() > LobbyConstants.EMAIL_MAX_LENGTH) {
      return String.format(
          "Email address has length %d exceeds the maximum length : %d",
          emailAddress.length(), LobbyConstants.EMAIL_MAX_LENGTH);
    }
    if (!emailAddress.matches(EMAIL_REGEX)) {
      return String.format("Email address is invalid: %s", emailAddress);
    }
    return null;
  }
}
