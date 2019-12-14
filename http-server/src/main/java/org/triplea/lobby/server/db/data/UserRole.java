package org.triplea.lobby.server.db.data;

import lombok.experimental.UtilityClass;

/**
 * Class to hold constants representing the possible set of user role values. <br>
 * Note: this is not an enum to allow these values to be referenced from annotations.
 */
@UtilityClass
public final class UserRole {
  public static final String ADMIN = "ADMIN";
  public static final String MODERATOR = "MODERATOR";
  public static final String PLAYER = "PLAYER";
  public static final String ANONYMOUS = "ANONYMOUS";
  public static final String HOST = "HOST";

  // TODO: Project#12 test-me
  public static boolean isModerator(final String roleName) {
    return ADMIN.equals(roleName) || MODERATOR.equals(roleName);
  }
}
