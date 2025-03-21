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
  public static final int PASSWORD_MIN_LENGTH = 3;
}
