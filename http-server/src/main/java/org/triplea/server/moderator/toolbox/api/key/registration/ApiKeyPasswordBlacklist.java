package org.triplea.server.moderator.toolbox.api.key.registration;

import java.util.function.Predicate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

/**
 * A list of hardcoded values deemed to be too easy to guess and should not be used for a password.
 */
public class ApiKeyPasswordBlacklist implements Predicate<String> {

  @VisibleForTesting
  static final ImmutableSet<String> PASSWORD_BLACKLIST =
      ImmutableSet.of(
          "triplea",
          "triple-a",
          "axisandallies",
          "moderator",
          "api-key",
          "1234",
          "pass",
          "password",
          "password1",
          "passw0rd",
          "qwerty",
          "abcd",
          "monkey",
          "batman",
          "football",
          "baseball",
          "login",
          "1111",
          "11111",
          "!@#$");


  @Override
  public boolean test(final String testPassword) {
    return PASSWORD_BLACKLIST.contains(testPassword.trim().toLowerCase());
  }
}
