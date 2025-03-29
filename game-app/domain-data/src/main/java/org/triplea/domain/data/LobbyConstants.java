package org.triplea.domain.data;

import lombok.experimental.UtilityClass;

/**
 * Class that stores constants related to the lobby, lobby users, etc... Note, changes to max or min
 * lengths might require database changes, notably column sizes.
 */
@UtilityClass
public class LobbyConstants {
  public static final int USERNAME_MIN_LENGTH = 3;
  public static final int USERNAME_MAX_LENGTH = 40;
  // The maximum email address length is 254
  // https://www.directedignorance.com/blog/maximum-length-of-email-address.
  public static final int EMAIL_MAX_LENGTH = 254;
  public static final int PASSWORD_MIN_LENGTH = 3;
}
